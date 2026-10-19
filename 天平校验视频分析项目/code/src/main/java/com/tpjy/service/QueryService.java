package com.tpjy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.Constants;
import com.tpjy.dto.CurveData;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.PageResult;
import com.tpjy.dto.RecordDetail;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.ReadTrace;
import com.tpjy.entity.Reading;
import com.tpjy.entity.SysUser;
import com.tpjy.entity.WeightBox;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.ReadTraceMapper;
import com.tpjy.mapper.ReadingMapper;
import com.tpjy.mapper.SysUserMapper;
import com.tpjy.mapper.WeightBoxMapper;
import com.tpjy.util.TpjyUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 记录查询与统计（WBS 1.3.6，甲）。
 *
 * <p>FR-015：按天平 + 日期段查询，默认只出「已复核」，已作废记录可另行调出。
 * FR-016：生成称重值曲线数据。FR-017：组装 PDF 导出所需的记录详情。
 */
@Service
public class QueryService {

    private final CalibrationRecordMapper recordMapper;
    private final ReadingMapper readingMapper;
    private final ReadTraceMapper traceMapper;
    private final BalanceMapper balanceMapper;
    private final SysUserMapper userMapper;
    private final WeightBoxMapper weightBoxMapper;
    private final JudgeService judgeService;
    private final CurveBuilder curveBuilder = new CurveBuilder();

    public QueryService(CalibrationRecordMapper recordMapper, ReadingMapper readingMapper,
                        ReadTraceMapper traceMapper, BalanceMapper balanceMapper,
                        SysUserMapper userMapper, WeightBoxMapper weightBoxMapper,
                        JudgeService judgeService) {
        this.recordMapper = recordMapper;
        this.readingMapper = readingMapper;
        this.traceMapper = traceMapper;
        this.balanceMapper = balanceMapper;
        this.userMapper = userMapper;
        this.weightBoxMapper = weightBoxMapper;
        this.judgeService = judgeService;
    }

    /**
     * 记录分页查询。
     *
     * @param status      指定状态；为 null 时默认「已复核」
     * @param includeVoid 是否含「已作废」记录（默认查询不含）
     */
    public PageResult<CalibrationRecord> page(Long balanceId, Date from, Date to,
                                             String status, boolean includeVoid,
                                             int page, int pageSize) {
        QueryWrapper<CalibrationRecord> w = new QueryWrapper<CalibrationRecord>();
        if (balanceId != null) {
            w.eq("balance_id", balanceId);
        }
        if (from != null) {
            w.ge("check_date", from);
        }
        if (to != null) {
            w.le("check_date", to);
        }
        final String effective = status != null ? status : Constants.STATUS_REVIEWED;
        if (includeVoid) {
            // 「含作废」仍只显示目标状态 + 已作废，不能把草稿/待复核等状态一并查出。
            w.and(q -> q.eq("status", effective).or().eq("status", Constants.STATUS_VOID));
        } else {
            w.eq("status", effective);
        }
        long total = recordMapper.selectCount(w);
        int p = page <= 0 ? 1 : page;
        int size = pageSize <= 0 ? 10 : pageSize;
        w.orderByDesc("check_date").orderByDesc("id");
        w.last("limit " + ((p - 1) * size) + "," + size);
        List<CalibrationRecord> list = recordMapper.selectList(w);
        for (CalibrationRecord r : list) {
            fillDisplay(r);
        }
        return PageResult.of(total, p, size, list);
    }

    /** 记录详情（读数表 + 判定 + 曲线 + 留痕），供详情页与 PDF 导出使用。 */
    public RecordDetail detail(Long recordId) {
        CalibrationRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("记录不存在：" + recordId);
        }
        fillDisplay(record);
        Balance balance = record.getBalanceId() == null ? null : balanceMapper.selectById(record.getBalanceId());
        List<Reading> readings = listReadings(recordId);
        List<RecognizedReading> forJudge = toRecognized(readings);

        BigDecimal w1 = balance == null ? null : balance.getWeightL1();
        BigDecimal w2 = balance == null ? null : balance.getWeightL2();
        BigDecimal w3 = balance == null ? null : balance.getWeightL3();
        JudgeResult judge = judgeService.judgeRecord(forJudge, w1, w2, w3,
                record.getCleanOk() != null && record.getCleanOk() == 1,
                record.getLevelOk() != null && record.getLevelOk() == 1,
                record.getInternalCalibOk());

        CurveData curve = curveBuilder.build(record.getRecordNo(),
                balance == null ? null : balance.getBalanceCode(),
                TpjyUtils.formatDate(record.getCheckDate()), judge);
        curveBuilder.fillClock(curve, forJudge);

        RecordDetail d = new RecordDetail();
        d.setRecordId(record.getId());
        d.setRecordNo(record.getRecordNo());
        d.setCheckDate(TpjyUtils.formatDate(record.getCheckDate()));
        d.setPerson(record.getCheckerName());
        d.setReviewerName(record.getReviewerName());
        d.setStatus(record.getStatus());
        d.setRemark(record.getRemark());
        d.setCleanOk(record.getCleanOk());
        d.setLevelOk(record.getLevelOk());
        d.setInternalCalibOk(record.getInternalCalibOk());
        d.setJudge(judge);
        d.setCurve(curve);
        if (balance != null) {
            d.setBalanceCode(balance.getBalanceCode());
            d.setDept(balance.getDept());
            d.setUniqueNo(balance.getUniqueNo());
            d.setPrecisionText(balance.getRange1Precision());
            d.setRangeText("Max " + nvl(balance.getRange1()) + " · d " + nvl(balance.getRange1Precision()));
            if (balance.getRange2() != null && !balance.getRange2().trim().isEmpty()) {
                d.setRangeText(d.getRangeText() + " / Max " + balance.getRange2()
                        + " · d " + nvl(balance.getRange2Precision()));
            }
            if (balance.getWeightBoxId() != null) {
                WeightBox box = weightBoxMapper.selectById(balance.getWeightBoxId());
                if (box != null) {
                    d.setWeightBoxNo(box.getBoxNo());
                }
            }
        }
        for (ReadTrace t : listTraces(recordId)) {
            d.getTraces().add(t.getOriginalValue() + " → " + t.getConfirmedValue());
        }
        return d;
    }

    /** 曲线数据（供查询页 ECharts）。 */
    public CurveData curve(Long recordId) {
        return detail(recordId).getCurve();
    }

    private List<Reading> listReadings(Long recordId) {
        QueryWrapper<Reading> w = new QueryWrapper<Reading>();
        w.eq("record_id", recordId).orderByAsc("seq");
        return readingMapper.selectList(w);
    }

    private List<ReadTrace> listTraces(Long recordId) {
        QueryWrapper<ReadTrace> w = new QueryWrapper<ReadTrace>();
        w.eq("record_id", recordId).orderByAsc("id");
        return traceMapper.selectList(w);
    }

    private List<RecognizedReading> toRecognized(List<Reading> readings) {
        List<RecognizedReading> list = new ArrayList<RecognizedReading>();
        for (Reading r : readings) {
            RecognizedReading rr = new RecognizedReading();
            rr.setSeq(r.getSeq() == null ? 0 : r.getSeq());
            rr.setLevel(r.getWeightLevel());
            rr.setNominal(r.getNominal());
            rr.setValue(r.getReadingValue());
            rr.setRawValue(r.getReadingValue() == null ? null : r.getReadingValue().toPlainString());
            if (r.getDisplayValue() != null) {
                rr.setRawValue(r.getDisplayValue());
            }
            rr.setDisplayUnit(r.getDisplayUnit());
            rr.setMeasureTime(TpjyUtils.formatDate(r.getMeasureTime()));
            rr.setChanged(r.getChanged() != null && r.getChanged());
            list.add(rr);
        }
        return list;
    }

    private void fillDisplay(CalibrationRecord r) {
        if (r.getBalanceId() != null) {
            Balance b = balanceMapper.selectById(r.getBalanceId());
            if (b != null) {
                r.setBalanceCode(b.getBalanceCode());
                r.setDept(b.getDept());
                r.setUniqueNo(b.getUniqueNo());
            }
        }
        if (r.getCheckerId() != null) {
            SysUser checker = userMapper.selectById(r.getCheckerId());
            if (checker != null) {
                r.setCheckerName(checker.getRealName());
            }
        }
        if (r.getReviewerId() != null) {
            SysUser reviewer = userMapper.selectById(r.getReviewerId());
            if (reviewer != null) {
                r.setReviewerName(reviewer.getRealName());
            }
        }
    }

    private static String nvl(String s) {
        return s == null ? "-" : s;
    }
}
