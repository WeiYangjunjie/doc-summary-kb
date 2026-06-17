package com.example.dockb.controller;

import com.example.dockb.common.Result;
import com.example.dockb.dto.ChangePasswordRequest;
import com.example.dockb.dto.LoginRequest;
import com.example.dockb.dto.RegisterRequest;
import com.example.dockb.service.AuthService;
import com.example.dockb.util.AuthContext;
import com.example.dockb.vo.AuthVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：注册 / 登录 / 修改密码。
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
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<AuthVO> login(@Valid @RequestBody LoginRequest req) {
        try {
            return Result.success(authService.login(req));
        } catch (IllegalArgumentException e) {
            return Result.fail(401, e.getMessage());
        }
    }

    /** 修改当前登录用户的密码。 */
    @PutMapping("/password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                   HttpServletRequest request) {
        Long userId = AuthContext.getUserId(request);
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        try {
            authService.changePassword(userId, req);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
