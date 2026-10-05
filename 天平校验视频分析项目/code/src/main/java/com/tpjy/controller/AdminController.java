package com.tpjy.controller;

import com.tpjy.entity.SysUser;
import com.tpjy.mapper.SysUserMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.List;

/** 管理员（ADMIN）页面。TODO 迭代2 实现增删/重置密码。 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private SysUserMapper userMapper;

    @GetMapping("/users")
    public String users(Model model) {
        List<SysUser> users = userMapper.selectList(null);
        model.addAttribute("users", users);
        return "admin/users";
    }
}
