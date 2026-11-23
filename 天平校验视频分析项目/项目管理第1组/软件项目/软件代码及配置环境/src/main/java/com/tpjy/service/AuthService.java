package com.tpjy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tpjy.common.BizException;
import com.tpjy.common.LoginUser;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/** 登录认证（FR-001）。 */
@Service
public class AuthService {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    public LoginUser login(String username, String password) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || user.getEnabled() == null || user.getEnabled() != 1
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        LoginUser lu = new LoginUser();
        lu.setUserId(user.getId());
        lu.setUsername(user.getUsername());
        lu.setRealName(user.getRealName());
        lu.setCardCode(user.getCardCode());
        lu.setRole(user.getRole());
        return lu;
    }
}
