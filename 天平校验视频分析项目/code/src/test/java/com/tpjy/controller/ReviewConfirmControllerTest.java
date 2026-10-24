package com.tpjy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.Constants;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpSession;
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

    private HttpSession login(String username) throws Exception {
        MvcResult login = mvc.perform(post("/login")
                        .param("username", username)
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return login.getRequest().getSession(false);
    }
}
