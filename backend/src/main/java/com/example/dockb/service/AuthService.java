package com.example.dockb.service;

import com.example.dockb.dto.LoginRequest;
import com.example.dockb.dto.RegisterRequest;
import com.example.dockb.vo.AuthVO;

/**
 * 认证服务。
 */
public interface AuthService {

    /**
     * 注册新用户（默认角色 USER）。
     * @throws IllegalArgumentException 用户名已存在
     */
    AuthVO register(RegisterRequest req);

    /**
     * 用户登录。
     * @throws IllegalArgumentException 用户名或密码错误
     */
    AuthVO login(LoginRequest req);
}
