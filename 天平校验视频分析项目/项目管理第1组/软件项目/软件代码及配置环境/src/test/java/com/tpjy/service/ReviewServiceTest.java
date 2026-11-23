package com.tpjy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.BizException;
import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import com.tpjy.entity.AuditLog;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.AuditLogMapper;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 复核确认测试（WBS 1.3.4，成员乙，切片 3）。
 * 通过公共 seam {@link ReviewService#confirm} / {@link ReviewService#reject} 验证。
 */
@SpringBootTest
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private CalibrationRecordMapper recordMapper;
    @Autowired
    private BalanceMapper balanceMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("确认：待复核→已复核，写复核人与时间，记 REVIEW_CONFIRM 审计")
    void confirm_setsReviewedStatus() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser reviewer = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "qiyi"));
        CalibrationRecord r = insertRecord(balance, "JY20260629-910", Constants.STATUS_PENDING);

        CalibrationRecord confirmed = reviewService.confirm(r.getId(), loginUser(reviewer));

        assertEquals(Constants.STATUS_REVIEWED, confirmed.getStatus(), "确认后应为已复核");
        assertEquals(reviewer.getId(), confirmed.getReviewerId(), "应记录复核人");
        assertNotNull(confirmed.getReviewedAt(), "应记录复核时间");
        List<AuditLog> audits = auditLogMapper.selectList(
                new QueryWrapper<AuditLog>().eq("action", "REVIEW_CONFIRM").eq("target_id", r.getId()));
        assertEquals(1, audits.size(), "应记 1 条复核审计");
    }

    @Test
    @DisplayName("打回：待复核→已打回")
    void reject_setsRejectedStatus() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser reviewer = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "qiyi"));
        CalibrationRecord r = insertRecord(balance, "JY20260629-911", Constants.STATUS_PENDING);

        CalibrationRecord rejected = reviewService.reject(r.getId(), loginUser(reviewer), "打回原因");

        assertEquals(Constants.STATUS_REJECTED, rejected.getStatus(), "打回后应为已打回");
    }

    @Test
    @DisplayName("非待复核记录不可确认")
    void confirm_rejectsNonPending() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser reviewer = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "qiyi"));
        CalibrationRecord r = insertRecord(balance, "JY20260629-912", Constants.STATUS_REVIEWED);

        assertThrows(BizException.class, () -> reviewService.confirm(r.getId(), loginUser(reviewer)));
    }

    @Test
    @DisplayName("批量确认：多条待复核全部确认，记 BATCH_CONFIRM 审计")
    void batchConfirm_confirmsAllPending() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser reviewer = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "qiyi"));
        CalibrationRecord r1 = insertRecord(balance, "JY20260629-930", Constants.STATUS_PENDING);
        CalibrationRecord r2 = insertRecord(balance, "JY20260629-931", Constants.STATUS_PENDING);

        int count = reviewService.batchConfirm(
                Arrays.asList(r1.getId(), r2.getId()), loginUser(reviewer));

        assertEquals(2, count, "应确认 2 条待复核记录");
        assertEquals(Constants.STATUS_REVIEWED, recordMapper.selectById(r1.getId()).getStatus());
        assertEquals(Constants.STATUS_REVIEWED, recordMapper.selectById(r2.getId()).getStatus());
        List<AuditLog> audits = auditLogMapper.selectList(
                new QueryWrapper<AuditLog>().eq("action", "BATCH_CONFIRM"));
        boolean found = false;
        for (AuditLog a : audits) {
            if (a.getDetail() != null && a.getDetail().contains("JY20260629-930")) {
                found = true;
            }
        }
        assertTrue(found, "应有一条包含本次记录号的批量确认审计");
    }

    @Test
    @DisplayName("批量确认：跳过非待复核记录")
    void batchConfirm_skipsNonPending() {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser reviewer = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "qiyi"));
        CalibrationRecord pending = insertRecord(balance, "JY20260629-932", Constants.STATUS_PENDING);
        CalibrationRecord reviewed = insertRecord(balance, "JY20260629-933", Constants.STATUS_REVIEWED);

        int count = reviewService.batchConfirm(
                Arrays.asList(pending.getId(), reviewed.getId()), loginUser(reviewer));

        assertEquals(1, count, "只确认 1 条待复核记录");
        assertEquals(Constants.STATUS_REVIEWED, recordMapper.selectById(pending.getId()).getStatus());
        assertEquals(Constants.STATUS_REVIEWED, recordMapper.selectById(reviewed.getId()).getStatus());
    }

    private CalibrationRecord insertRecord(Balance balance, String recordNo, String status) {
        CalibrationRecord r = new CalibrationRecord();
        r.setRecordNo(recordNo);
        r.setBalanceId(balance.getId());
        r.setCheckDate(new Date());
        r.setStatus(status);
        recordMapper.insert(r);
        return r;
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
