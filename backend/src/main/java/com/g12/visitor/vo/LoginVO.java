package com.g12.visitor.vo;

import lombok.Data;

/**
 * 登录响应：token + 用户信息。
 */
@Data
public class LoginVO {

    private String token;
    private UserVO user;
}
