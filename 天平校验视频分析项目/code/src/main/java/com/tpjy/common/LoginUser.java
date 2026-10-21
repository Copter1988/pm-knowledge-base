package com.tpjy.common;

import lombok.Data;

import java.io.Serializable;

/** 会话中保存的登录用户信息。 */
@Data
public class LoginUser implements Serializable {
    private Long userId;
    private String username;
    private String realName;
    private String cardCode;
    private String role;
}
