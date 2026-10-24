package com.tpjy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 账号维护集成测试（WBS 1.3.1，成员乙，切片 4）。
 * 通过公共 seam {@link AdminController} 的增/停用端点验证。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private SysUserMapper userMapper;

    @Test
    @DisplayName("新增账号后可用默认密码登录")
    void addUser_thenCanLogin() throws Exception {
        HttpSession admin = login("admin");

        mvc.perform(post("/admin/users/add")
                        .param("username", "zhangsan")
                        .param("realName", "张三")
                        .param("role", "CHECKER")
                        .param("cardCode", "JY009")
                        .session((org.springframework.mock.web.MockHttpSession) admin))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/login").param("username", "zhangsan").param("password", "123456"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("停用账号后不能登录")
    void disableUser_thenCannotLogin() throws Exception {
        HttpSession admin = login("admin");
        SysUser lier = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "lier"));

        mvc.perform(post("/admin/users/disable")
                        .param("userId", String.valueOf(lier.getId()))
                        .session((org.springframework.mock.web.MockHttpSession) admin))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/login").param("username", "lier").param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("账号或密码错误")));
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
