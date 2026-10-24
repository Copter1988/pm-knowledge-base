package com.tpjy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.BizException;
import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.dto.StoredVideoInfo;
import com.tpjy.entity.AuditLog;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.Reading;
import com.tpjy.entity.ReadTrace;
import com.tpjy.entity.SysUser;
import com.tpjy.entity.Video;
import com.tpjy.mapper.AuditLogMapper;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.ReadingMapper;
import com.tpjy.mapper.ReadTraceMapper;
import com.tpjy.mapper.SysUserMapper;
import com.tpjy.mapper.VideoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验记录管理测试（WBS 1.3.3，成员乙，切片 1/2）。
 * 通过公共 seam {@link RecordService#createRecord} / {@link RecordService#abandon} 验证，
 * 不测内部实现。
 */
@SpringBootTest
class RecordServiceTest {

    @Autowired
    private RecordService recordService;
    @Autowired
    private BalanceMapper balanceMapper;
    @Autowired
    private CalibrationRecordMapper recordMapper;
    @Autowired
    private ReadingMapper readingMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private ReadTraceMapper traceMapper;
    @Autowired
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("提交一条身份匹配的记录：record + 9 reading + video 落库，状态待复核")
    void createRecord_persistsRecordAndReadings() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));

        RecognitionResult result = resultOf("李一", "F001", "2026-06-29 10:13");
        StoredVideoInfo video = new StoredVideoInfo(new File("target/test-upload/video/fake.mp4"),
                "upload/video/20260629/fake.mp4", "smoke.mp4", new Date());

        CalibrationRecord record = recordService.createRecord(
                result, video, balance.getId(), loginUser(checker),
                true, true, Constants.CALIB_YES, null, result.getReadings());

        // 记录主表
        assertNotNull(record.getId(), "记录应落库并回填主键");
        assertEquals(Constants.STATUS_PENDING, record.getStatus(), "提交后状态应为待复核");
        assertEquals(balance.getId(), record.getBalanceId(), "应绑定所选天平");
        assertTrue(record.getRecordNo().matches("JY20260629-\\d{3}"),
                "记录号应符合 JY+yyyyMMdd+3位流水，实际：" + record.getRecordNo());

        // 9 次示值，三列齐全
        List<Reading> readings = readingMapper.selectList(
                new QueryWrapper<Reading>().eq("record_id", record.getId()).orderByAsc("seq"));
        assertEquals(9, readings.size(), "应落 9 条示值");
        for (Reading r : readings) {
            assertNotNull(r.getReadingValue(), "第 " + r.getSeq() + " 次 reading_value 不能为空");
            assertNotNull(r.getDisplayValue(), "第 " + r.getSeq() + " 次 display_value 不能为空");
            assertNotNull(r.getDisplayUnit(), "第 " + r.getSeq() + " 次 display_unit 不能为空");
        }

        // 视频落一条
        List<Video> videos = videoMapper.selectList(
                new QueryWrapper<Video>().eq("record_id", record.getId()));
        assertEquals(1, videos.size(), "应落 1 条视频记录");
        assertEquals("smoke.mp4", videos.get(0).getOriginalName(), "视频原名应保留");
    }

    @Test
    @DisplayName("身份不一致（校验员卡不匹配）时阻断：抛异常且不落任何记录")
    void createRecord_blocksWhenIdentityMismatch() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));

        RecognitionResult result = resultOf("李一", "F001", "2026-06-29 10:13");
        result.addAlert("CHECKER_MISMATCH", "WARN", "视频检验员身份和登录身份不匹配，不予校验", null);
        StoredVideoInfo video = new StoredVideoInfo(new File("target/test-upload/video/fake.mp4"),
                "upload/video/20260629/fake.mp4", "smoke.mp4", new Date());

        long before = recordMapper.selectCount(null);

        assertThrows(BizException.class, () -> recordService.createRecord(
                result, video, balance.getId(), loginUser(checker),
                true, true, Constants.CALIB_YES, null, result.getReadings()));

        assertEquals(before, recordMapper.selectCount(null), "身份不一致时不得落任何记录");
    }

    @Test
    @DisplayName("改值留痕：识别原值与确认值不一致时写 read_trace 并记 READ_CHANGE 审计")
    void createRecord_writesReadTraceOnChange() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));

        RecognitionResult result = resultOf("李一", "F001", "2026-06-29 10:13");
        List<RecognizedReading> edited = cloneReadings(result.getReadings());
        edited.get(0).setRawValue("200.0011");
        edited.get(0).setValue(new BigDecimal("200.0011"));

        StoredVideoInfo video = new StoredVideoInfo(new File("target/test-upload/video/fake.mp4"),
                "upload/video/20260629/fake.mp4", "smoke.mp4", new Date());

        CalibrationRecord record = recordService.createRecord(
                result, video, balance.getId(), loginUser(checker),
                true, true, Constants.CALIB_YES, null, edited);

        List<ReadTrace> traces = traceMapper.selectList(
                new QueryWrapper<ReadTrace>().eq("record_id", record.getId()));
        assertEquals(1, traces.size(), "只改 1 次示值，应落 1 条留痕");
        assertTrue(traces.get(0).getOriginalValue().contains("200.0006"), "原值应保留识别值");
        assertTrue(traces.get(0).getConfirmedValue().contains("200.0011"), "确认值应是改动后的值");

        List<AuditLog> audits = auditLogMapper.selectList(
                new QueryWrapper<AuditLog>().eq("action", "READ_CHANGE").eq("target_id", record.getId()));
        assertEquals(1, audits.size(), "应记 1 条 READ_CHANGE 审计");
    }

    @Test
    @DisplayName("作废记录：状态变为已作废、保留在库、记 ABANDON 审计")
    void abandon_setsVoidStatus() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));

        RecognitionResult result = resultOf("李一", "F001", "2026-06-29 10:13");
        StoredVideoInfo video = new StoredVideoInfo(new File("target/test-upload/video/fake.mp4"),
                "upload/video/20260629/fake.mp4", "smoke.mp4", new Date());
        CalibrationRecord record = recordService.createRecord(
                result, video, balance.getId(), loginUser(checker),
                true, true, Constants.CALIB_YES, null, result.getReadings());

        CalibrationRecord abandoned = recordService.abandon(record.getId(), loginUser(checker), "作废原因");

        assertEquals(Constants.STATUS_VOID, abandoned.getStatus(), "作废后状态应为已作废");
        assertNotNull(recordMapper.selectById(record.getId()), "作废记录应保留在库");
        List<AuditLog> audits = auditLogMapper.selectList(
                new QueryWrapper<AuditLog>().eq("action", "ABANDON").eq("target_id", record.getId()));
        assertEquals(1, audits.size(), "应记 1 条 ABANDON 审计");
    }

    @Test
    @DisplayName("已复核记录不可作废")
    void abandon_reviewedRecordCannotAbandon() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));

        CalibrationRecord record = new CalibrationRecord();
        record.setRecordNo("JY20260629-900");
        record.setBalanceId(balance.getId());
        record.setCheckDate(new Date());
        record.setStatus(Constants.STATUS_REVIEWED);
        recordMapper.insert(record);

        assertThrows(BizException.class, () ->
                recordService.abandon(record.getId(), loginUser(checker), "x"));
    }

    private List<RecognizedReading> cloneReadings(List<RecognizedReading> source) {
        List<RecognizedReading> out = new ArrayList<RecognizedReading>();
        for (RecognizedReading r : source) {
            RecognizedReading c = new RecognizedReading();
            c.setSeq(r.getSeq());
            c.setLevel(r.getLevel());
            c.setNominal(r.getNominal());
            c.setValue(r.getValue());
            c.setRawValue(r.getRawValue());
            c.setDisplayUnit(r.getDisplayUnit());
            c.setMeasureTime(r.getMeasureTime());
            out.add(c);
        }
        return out;
    }

    private RecognitionResult resultOf(String person, String balanceCode, String checkDate) {
        RecognitionResult result = new RecognitionResult();
        result.setEffectivePersonName(person);
        result.setEffectiveBalanceCode(balanceCode);
        result.setCheckDate(checkDate);
        result.setReadings(goodReadings());
        return result;
    }

    private List<RecognizedReading> goodReadings() {
        String[] values = {"200.0006", "199.9998", "200.0011",
                "50.0003", "49.9997", "50.0002", "10.0001", "10.0000", "9.9999"};
        List<RecognizedReading> list = new ArrayList<RecognizedReading>();
        for (int i = 0; i < values.length; i++) {
            RecognizedReading r = new RecognizedReading();
            r.setSeq(i + 1);
            r.setLevel(levelOf(i + 1));
            r.setRawValue(values[i]);
            r.setValue(new BigDecimal(values[i]));
            r.setDisplayUnit("g");
            r.setMeasureTime("2026-06-29 10:" + String.format("%02d", 13 + i));
            list.add(r);
        }
        return list;
    }

    private String levelOf(int seq) {
        if (seq <= 3) {
            return "L1";
        }
        if (seq <= 6) {
            return "L2";
        }
        return "L3";
    }

    private LoginUser loginUser(SysUser u) {
        LoginUser lu = new LoginUser();
        lu.setUserId(u.getId());
        lu.setUsername(u.getUsername());
        lu.setRealName(u.getRealName());
        lu.setCardCode(u.getCardCode());
        lu.setRole(u.getRole());
        return lu;
    }
}
