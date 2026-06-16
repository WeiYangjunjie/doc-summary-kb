package com.example.dockb.controller;

import com.example.dockb.common.Result;
import com.example.dockb.dto.LoginRequest;
import com.example.dockb.dto.RegisterRequest;
import com.example.dockb.service.AuthService;
import com.example.dockb.vo.AuthVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：注册 / 登录。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AuthVO> register(@Valid @RequestBody RegisterRequest req) {
        try {
            return Result.success(authService.register(req));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<AuthVO> login(@Valid @RequestBody LoginRequest req) {
        try {
            return Result.success(authService.login(req));
        } catch (IllegalArgumentException e) {
            return Result.error(401, e.getMessage());
        }
    }
}
