package com.example.dockb.vo;

import lombok.Data;

/**
 * 登录/注册 响应。
 */
@Data
public class AuthVO {

    private Long userId;
    private String username;
    private String role;
    private String token;

    public AuthVO(Long userId, String username, String role, String token) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.token = token;
    }
}
