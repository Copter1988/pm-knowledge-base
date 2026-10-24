package com.tpjy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.List;

/** 管理员（ADMIN）页面：账号增删、停用/启用、重置密码。 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", listUsers());
        return "admin/users";
    }

    @GetMapping("/users/edit")
    public String editForm(@RequestParam("id") Long id, Model model) {
        model.addAttribute("user", userMapper.selectById(id));
        return "admin/user-edit";
    }

    @PostMapping("/users/edit")
    public String edit(@RequestParam("userId") Long userId,
                       @RequestParam("realName") String realName,
                       @RequestParam("role") String role,
                       @RequestParam(value = "cardCode", required = false) String cardCode) {
        SysUser u = userMapper.selectById(userId);
        if (u != null) {
            u.setRealName(realName);
            u.setRole(role);
            u.setCardCode(cardCode);
            userMapper.updateById(u);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/add")
    public String addUser(@RequestParam("username") String username,
                          @RequestParam("realName") String realName,
                          @RequestParam("role") String role,
                          @RequestParam(value = "cardCode", required = false) String cardCode,
                          Model model) {
        if (userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username)) != null) {
            model.addAttribute("error", "账号已存在：" + username);
            model.addAttribute("users", listUsers());
            return "admin/users";
        }
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setCardCode(cardCode);
        u.setRole(role);
        u.setEnabled(1);
        userMapper.insert(u);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/disable")
    public String disableUser(@RequestParam("userId") Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u != null) {
            u.setEnabled(0);
            userMapper.updateById(u);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/enable")
    public String enableUser(@RequestParam("userId") Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u != null) {
            u.setEnabled(1);
            userMapper.updateById(u);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/reset-password")
    public String resetPassword(@RequestParam("userId") Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u != null) {
            u.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(u);
        }
        return "redirect:/admin/users";
    }

    private List<SysUser> listUsers() {
        return userMapper.selectList(new QueryWrapper<SysUser>().orderByAsc("id"));
    }
}
