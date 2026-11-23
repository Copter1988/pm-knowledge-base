package com.tpjy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.entity.Balance;
import com.tpjy.mapper.BalanceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 主数据维护集成测试（WBS 1.3.7，切片 5 的 UI 接线）。
 * 验证新增天平后列表可见。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MasterDataControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BalanceMapper balanceMapper;

    @Test
    @DisplayName("新增天平后列表可见")
    void addBalance_thenListed() throws Exception {
        HttpSession admin = login("admin");

        mvc.perform(post("/admin/masterdata/add")
                        .param("balanceCode", "F100")
                        .param("dept", "测试室")
                        .param("uniqueNo", "U100")
                        .param("range1", "220g")
                        .param("range1Precision", "0.1mg")
                        .param("weightL1", "200")
                        .param("weightL2", "50")
                        .param("weightL3", "10")
                        .session((org.springframework.mock.web.MockHttpSession) admin))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/admin/masterdata")
                        .session((org.springframework.mock.web.MockHttpSession) admin))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("F100")));
    }

    @Test
    @DisplayName("新增双量程天平：range2/range2Precision 保存")
    void addBalance_supportsDoubleRange() throws Exception {
        HttpSession admin = login("admin");

        mvc.perform(post("/admin/masterdata/add")
                        .param("balanceCode", "F101")
                        .param("dept", "测试室")
                        .param("uniqueNo", "U101")
                        .param("range1", "120g")
                        .param("range1Precision", "0.1mg")
                        .param("range2", "41g")
                        .param("range2Precision", "0.01mg")
                        .param("weightL1", "100")
                        .param("weightL2", "20")
                        .param("weightL3", "2")
                        .session((org.springframework.mock.web.MockHttpSession) admin))
                .andExpect(status().is3xxRedirection());

        Balance loaded = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F101"));
        assertEquals("41g", loaded.getRange2());
        assertEquals("0.01mg", loaded.getRange2Precision());
    }

    @Test
    @DisplayName("编辑天平：砝码标称值更新")
    void editBalance_updates() throws Exception {
        HttpSession admin = login("admin");
        mvc.perform(post("/admin/masterdata/add")
                        .param("balanceCode", "F102")
                        .param("dept", "测试室")
                        .param("uniqueNo", "U102")
                        .param("range1", "220g")
                        .param("range1Precision", "0.1mg")
                        .param("weightL1", "200")
                        .param("weightL2", "50")
                        .param("weightL3", "10")
                        .session((org.springframework.mock.web.MockHttpSession) admin))
                .andExpect(status().is3xxRedirection());
        Balance b = balanceMapper.selectOne(new QueryWrapper<Balance>().eq("balance_code", "F102"));

        mvc.perform(post("/admin/masterdata/edit")
                        .param("id", String.valueOf(b.getId()))
                        .param("balanceCode", "F102")
                        .param("dept", "测试室")
                        .param("uniqueNo", "U102")
                        .param("range1", "220g")
                        .param("range1Precision", "0.1mg")
                        .param("weightL1", "500")
                        .param("weightL2", "50")
                        .param("weightL3", "10")
                        .session((org.springframework.mock.web.MockHttpSession) admin))
                .andExpect(status().is3xxRedirection());

        assertEquals(0, new BigDecimal("500").compareTo(balanceMapper.selectById(b.getId()).getWeightL1()));
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
