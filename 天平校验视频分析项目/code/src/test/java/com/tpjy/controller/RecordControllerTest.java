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
 * 校验记录管理集成测试（WBS 1.3.3，切片 2 的作废 UI 接线）。
 * 验证 /checker/record/abandon 作废端点。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecordControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CalibrationRecordMapper recordMapper;
    @Autowired
    private BalanceMapper balanceMapper;
    @Autowired
    private SysUserMapper userMapper;

    @Test
    @DisplayName("作废端点：POST 后记录状态变已作废")
    void abandonEndpoint() throws Exception {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));
        CalibrationRecord r = new CalibrationRecord();
        r.setRecordNo("JY20260629-950");
        r.setBalanceId(balance.getId());
        r.setCheckerId(checker.getId());
        r.setCheckDate(new Date());
        r.setStatus(Constants.STATUS_PENDING);
        recordMapper.insert(r);

        HttpSession session = login("liyi");

        mvc.perform(post("/checker/record/abandon")
                        .param("recordId", String.valueOf(r.getId()))
                        .param("reason", "录错了")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().is3xxRedirection());

        assertEquals(Constants.STATUS_VOID, recordMapper.selectById(r.getId()).getStatus(),
                "作废后状态应为已作废");
    }

    @Test
    @DisplayName("我的记录列表：渲染当前检验人员的待复核记录")
    void listShowsOwnRecords() throws Exception {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));
        CalibrationRecord r = new CalibrationRecord();
        r.setRecordNo("JY20260629-951");
        r.setBalanceId(balance.getId());
        r.setCheckerId(checker.getId());
        r.setCheckDate(new Date());
        r.setStatus(Constants.STATUS_PENDING);
        recordMapper.insert(r);

        HttpSession session = login("liyi");

        mvc.perform(get("/checker/record/list")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JY20260629-951")));
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
