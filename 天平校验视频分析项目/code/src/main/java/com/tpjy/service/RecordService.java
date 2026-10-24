package com.tpjy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.BizException;
import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.Operator;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.dto.ReadingJudge;
import com.tpjy.dto.StoredVideoInfo;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.ReadTrace;
import com.tpjy.entity.Reading;
import com.tpjy.entity.Video;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.ReadTraceMapper;
import com.tpjy.mapper.ReadingMapper;
import com.tpjy.mapper.VideoMapper;
import com.tpjy.util.TpjyUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校验记录管理（WBS 1.3.3，成员乙）。
 * 新建（导入 → 核对 → 提交落库）、改值留痕（read_trace + 审计）、作废（保留可查询）。
 *
 * <p>联动甲已注册的 JudgeService（判定）、AuditService（留痕）与各 Mapper；
 * 不改动甲的任何已实现类，只在本类补齐记录落库闭环。
 */
@Service
public class RecordService {

    private static final String DATE_TIME = "yyyy-MM-dd HH:mm";

    private final CalibrationRecordMapper recordMapper;
    private final ReadingMapper readingMapper;
    private final ReadTraceMapper traceMapper;
    private final VideoMapper videoMapper;
    private final BalanceMapper balanceMapper;
    private final JudgeService judgeService;
    private final AuditService auditService;

    public RecordService(CalibrationRecordMapper recordMapper, ReadingMapper readingMapper,
                         ReadTraceMapper traceMapper, VideoMapper videoMapper,
                         BalanceMapper balanceMapper, JudgeService judgeService,
                         AuditService auditService) {
        this.recordMapper = recordMapper;
        this.readingMapper = readingMapper;
        this.traceMapper = traceMapper;
        this.videoMapper = videoMapper;
        this.balanceMapper = balanceMapper;
        this.judgeService = judgeService;
        this.auditService = auditService;
    }

    /**
     * 新建校验记录：身份阻断校验 → 判定 → 落 video/calibration_record/reading → 改值留痕。
     *
     * @param result         识别结果（含原始 readings，用于改值对比）
     * @param video          已落盘的视频信息
     * @param balanceId      所选天平
     * @param checker        当前登录检验人员
     * @param cleanOk        清洁是否符合要求
     * @param levelOk        水平调节是否到位
     * @param calibOk        内部校正：是 / 否 / NA（NA 不算不通过）
     * @param remark         备注（为空时采用 JudgeResult 预填）
     * @param editedReadings 9 次最终示值（seq 1~9，含人工改值）
     */
    public CalibrationRecord createRecord(RecognitionResult result, StoredVideoInfo video,
                                          Long balanceId, LoginUser checker,
                                          boolean cleanOk, boolean levelOk, String calibOk,
                                          String remark, List<RecognizedReading> editedReadings) {
        if (result == null) {
            throw new BizException("缺少识别结果，请重新导入视频");
        }
        if (!result.canCreateRecord()) {
            throw new BizException(result.getBlockingMessage() != null
                    ? result.getBlockingMessage() : "身份不一致，无法建立校验记录");
        }
        Balance balance = balanceMapper.selectById(balanceId);
        if (balance == null) {
            throw new BizException("所选天平不存在");
        }

        BigDecimal w1 = balance.getWeightL1();
        BigDecimal w2 = balance.getWeightL2();
        BigDecimal w3 = balance.getWeightL3();
        List<RecognizedReading> finalReadings = normalizeReadings(editedReadings);
        JudgeResult judge = judgeService.judgeRecord(finalReadings, w1, w2, w3,
                cleanOk, levelOk, calibOk);

        Date checkDate = parseDateTime(result.getCheckDate());
        if (checkDate == null) {
            checkDate = new Date();
        }

        CalibrationRecord record = new CalibrationRecord();
        record.setRecordNo(TpjyUtils.recordNo(checkDate, nextDailySeq(checkDate)));
        record.setBalanceId(balanceId);
        record.setCheckDate(checkDate);
        record.setCleanOk(cleanOk ? 1 : 0);
        record.setLevelOk(levelOk ? 1 : 0);
        record.setInternalCalibOk(calibOk);
        record.setResult(judge.isQualified() ? 1 : 0);
        record.setRemark(blankToNull(remark) == null ? judge.getRemarkPrefill() : remark.trim());
        record.setCheckerId(checker == null ? null : checker.getUserId());
        record.setStatus(Constants.STATUS_PENDING);
        record.setConfirmFlag(0);
        record.setSubmittedAt(new Date());
        record.setCreatedAt(new Date());
        recordMapper.insert(record);

        // 视频（deleted_at 留空，由 VideoRetentionService 清理时写入）
        Video videoRow = new Video();
        videoRow.setRecordId(record.getId());
        videoRow.setFilePath(video.getRelativePath());
        videoRow.setOriginalName(video.getOriginalName());
        videoRow.setUploadedAt(video.getStoredAt());
        videoMapper.insert(videoRow);

        // 9 次示值（reading_value 存换算克值，display_value/display_unit 保原屏）
        Map<Integer, ReadingJudge> details = new HashMap<Integer, ReadingJudge>();
        for (ReadingJudge d : judge.getDetails()) {
            details.put(d.getSeq(), d);
        }
        Map<Integer, Long> readingIds = new HashMap<Integer, Long>();
        for (RecognizedReading r : finalReadings) {
            String level = r.getLevel() != null ? r.getLevel() : judgeService.groupLevel(r.getSeq());
            Reading row = new Reading();
            row.setRecordId(record.getId());
            row.setSeq(r.getSeq());
            row.setWeightLevel(level);
            row.setNominal(nominalOf(level, w1, w2, w3));
            row.setReadingValue(r.getValue());
            row.setDisplayValue(r.getRawValue());
            row.setDisplayUnit(r.getDisplayUnit());
            row.setMeasureTime(parseDateTime(r.getMeasureTime()));
            ReadingJudge dj = details.get(r.getSeq());
            if (dj != null) {
                row.setIsQualified(dj.isQualified() ? 1 : 0);
            }
            readingMapper.insert(row);
            readingIds.put(r.getSeq(), row.getId());
        }

        // 改值留痕：对比识别原值 vs 最终确认值，仅改动项写 read_trace + 审计
        Operator operator = operatorOf(checker);
        Map<Integer, RecognizedReading> originals = originalReadings(result);
        for (RecognizedReading r : finalReadings) {
            RecognizedReading original = originals.get(r.getSeq());
            if (!changedFrom(original, r)) {
                continue;
            }
            String originalText = displayText(original);
            String confirmedText = displayText(r);
            ReadTrace trace = new ReadTrace();
            trace.setRecordId(record.getId());
            trace.setReadingId(readingIds.get(r.getSeq()));
            trace.setOriginalValue(originalText);
            trace.setConfirmedValue(confirmedText);
            trace.setChangedBy(checker == null ? null : checker.getUserId());
            trace.setChangedAt(new Date());
            traceMapper.insert(trace);
            auditService.logReadChange(operator, record.getId(), r.getSeq(),
                    originalText, confirmedText);
        }
        return record;
    }

    /** 作废记录：保留在库、默认不展示、不记作废原因（原因只进审计）。 */
    public CalibrationRecord abandon(Long recordId, LoginUser checker, String reason) {
        CalibrationRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException("记录不存在：" + recordId);
        }
        if (Constants.STATUS_REVIEWED.equals(record.getStatus())) {
            throw new BizException("已复核记录不可作废");
        }
        record.setStatus(Constants.STATUS_VOID);
        recordMapper.updateById(record);
        auditService.logAbandon(operatorOf(checker), recordId, reason);
        return record;
    }

    // ---- 内部工具 ----

    /** 把示值按 seq 1~9 归一化，缺失的补空占位，保证判定与落库序号一致。 */
    private List<RecognizedReading> normalizeReadings(List<RecognizedReading> readings) {
        Map<Integer, RecognizedReading> bySeq = new HashMap<Integer, RecognizedReading>();
        if (readings != null) {
            for (RecognizedReading r : readings) {
                if (r != null && r.getSeq() >= 1 && r.getSeq() <= 9) {
                    bySeq.put(r.getSeq(), r);
                }
            }
        }
        List<RecognizedReading> out = new ArrayList<RecognizedReading>();
        for (int seq = 1; seq <= 9; seq++) {
            RecognizedReading r = bySeq.get(seq);
            if (r == null) {
                r = new RecognizedReading();
                r.setSeq(seq);
                r.setLevel(judgeService.groupLevel(seq));
            }
            out.add(r);
        }
        return out;
    }

    private Map<Integer, RecognizedReading> originalReadings(RecognitionResult result) {
        Map<Integer, RecognizedReading> map = new HashMap<Integer, RecognizedReading>();
        if (result.getReadings() != null) {
            for (RecognizedReading r : result.getReadings()) {
                if (r != null && r.getSeq() >= 1 && r.getSeq() <= 9) {
                    map.put(r.getSeq(), r);
                }
            }
        }
        return map;
    }

    private boolean changedFrom(RecognizedReading original, RecognizedReading edited) {
        if (original == null) {
            return edited.getValue() != null;
        }
        BigDecimal a = original.getValue();
        BigDecimal b = edited.getValue();
        if (a == null && b == null) {
            return false;
        }
        if (a == null || b == null) {
            return true;
        }
        return a.compareTo(b) != 0;
    }

    private String displayText(RecognizedReading r) {
        if (r == null || r.getValue() == null) {
            return "未识别";
        }
        String raw = r.getRawValue() != null ? r.getRawValue()
                : r.getValue().stripTrailingZeros().toPlainString();
        String unit = r.getDisplayUnit() == null ? "g" : r.getDisplayUnit();
        return raw + " " + unit;
    }

    private BigDecimal nominalOf(String level, BigDecimal w1, BigDecimal w2, BigDecimal w3) {
        if (Constants.LEVEL_L1.equals(level)) {
            return w1;
        }
        if (Constants.LEVEL_L2.equals(level)) {
            return w2;
        }
        return w3;
    }

    private int nextDailySeq(Date date) {
        String prefix = "JY" + TpjyUtils.yyyyMMdd(date) + "-";
        QueryWrapper<CalibrationRecord> w = new QueryWrapper<CalibrationRecord>();
        w.likeRight("record_no", prefix).orderByDesc("record_no");
        w.last("limit 1");
        List<CalibrationRecord> list = recordMapper.selectList(w);
        if (list == null || list.isEmpty()) {
            return 1;
        }
        String no = list.get(0).getRecordNo();
        int dash = no == null ? -1 : no.lastIndexOf('-');
        if (dash < 0) {
            return 1;
        }
        try {
            return Integer.parseInt(no.substring(dash + 1)) + 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private Date parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat(DATE_TIME);
            fmt.setLenient(false);
            return fmt.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Operator operatorOf(LoginUser user) {
        if (user == null) {
            return Operator.system();
        }
        return new Operator(user.getUserId(), user.getRealName(), user.getRole());
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
