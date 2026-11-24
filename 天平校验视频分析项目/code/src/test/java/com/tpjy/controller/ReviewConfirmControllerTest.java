package com.tpjy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.Constants;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.Reading;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.ReadingMapper;
import com.tpjy.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 复核确认集成测试（WBS 1.3.4，切片 3 的 UI 接线）。
 * 验证 /reviewer/review/pending 渲染 + confirm 后状态流转。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReviewConfirmControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CalibrationRecordMapper recordMapper;
    @Autowired
    private BalanceMapper balanceMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private ReadingMapper readingMapper;

    @Test
    @DisplayName("待复核列表渲染 + 确认后状态变已复核")
    void pendingAndConfirm() throws Exception {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));
        CalibrationRecord r = new CalibrationRecord();
        r.setRecordNo("JY20260629-920");
        r.setBalanceId(balance.getId());
        r.setCheckerId(checker.getId());
        r.setCheckDate(new Date());
        r.setStatus(Constants.STATUS_PENDING);
        recordMapper.insert(r);

        HttpSession session = login("qiyi");

        mvc.perform(get("/reviewer/review/pending")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JY20260629-920")));

        mvc.perform(post("/reviewer/review/confirm")
                        .param("recordId", String.valueOf(r.getId()))
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().is3xxRedirection());

        assertEquals(Constants.STATUS_REVIEWED, recordMapper.selectById(r.getId()).getStatus(),
                "确认后状态应为已复核");
    }

    @Test
    @DisplayName("批量确认：选中多条待复核记录，POST 后状态变已复核")
    void batchConfirm() throws Exception {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));
        CalibrationRecord r1 = new CalibrationRecord();
        r1.setRecordNo("JY20260629-940");
        r1.setBalanceId(balance.getId());
        r1.setCheckerId(checker.getId());
        r1.setCheckDate(new Date());
        r1.setStatus(Constants.STATUS_PENDING);
        recordMapper.insert(r1);
        CalibrationRecord r2 = new CalibrationRecord();
        r2.setRecordNo("JY20260629-941");
        r2.setBalanceId(balance.getId());
        r2.setCheckerId(checker.getId());
        r2.setCheckDate(new Date());
        r2.setStatus(Constants.STATUS_PENDING);
        recordMapper.insert(r2);

        HttpSession session = login("qiyi");

        mvc.perform(post("/reviewer/review/batch-confirm")
                        .param("recordIds", String.valueOf(r1.getId()), String.valueOf(r2.getId()))
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().is3xxRedirection());

        assertEquals(Constants.STATUS_REVIEWED, recordMapper.selectById(r1.getId()).getStatus());
        assertEquals(Constants.STATUS_REVIEWED, recordMapper.selectById(r2.getId()).getStatus());
    }

    @Test
    @DisplayName("复核列表可进入详情并查看前置条件、九次示值与判定")
    void reviewDetailShowsRecordContent() throws Exception {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));
        CalibrationRecord record = new CalibrationRecord();
        record.setRecordNo("JY20261124-REVIEW-DETAIL-001");
        record.setBalanceId(balance.getId());
        record.setCheckerId(checker.getId());
        record.setCheckDate(new Date());
        record.setCleanOk(1);
        record.setLevelOk(1);
        record.setInternalCalibOk("是");
        record.setStatus(Constants.STATUS_PENDING);
        record.setRemark("复核详情测试");
        recordMapper.insert(record);

        String[] values = {"200.0006", "199.9998", "200.0011", "50.0003", "49.9997",
                "50.0002", "10.0001", "10.0000", "9.9999"};
        String[] levels = {"L1", "L1", "L1", "L2", "L2", "L2", "L3", "L3", "L3"};
        String[] nominals = {"200", "200", "200", "50", "50", "50", "10", "10", "10"};
        for (int i = 0; i < 9; i++) {
            Reading reading = new Reading();
            reading.setRecordId(record.getId());
            reading.setSeq(i + 1);
            reading.setWeightLevel(levels[i]);
            reading.setNominal(new BigDecimal(nominals[i]));
            reading.setReadingValue(new BigDecimal(values[i]));
            reading.setDisplayValue(values[i]);
            reading.setDisplayUnit("g");
            reading.setMeasureTime(new Date());
            reading.setIsQualified(1);
            readingMapper.insert(reading);
        }

        HttpSession session = login("qiyi");
        mvc.perform(get("/reviewer/review/pending")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("查看详情")));

        mvc.perform(get("/reviewer/review/detail")
                        .param("recordId", String.valueOf(record.getId()))
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("复核记录详情")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JY20261124-REVIEW-DETAIL-001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("称量前置条件")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("九次称量值")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("200.0006 g")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("复核详情测试")));
    }

    private HttpSession login(String username) throws Exception {
        MvcResult login = mvc.perform(post("/login")
                        .param("username", username)
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return login.getRequest().getSession(false);
    }
}
