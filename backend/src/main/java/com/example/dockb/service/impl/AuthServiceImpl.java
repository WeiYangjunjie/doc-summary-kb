package com.example.dockb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dockb.common.BizException;
import com.example.dockb.common.ResultCode;
import com.example.dockb.dto.ChangePasswordRequest;
import com.example.dockb.dto.LoginRequest;
import com.example.dockb.dto.RegisterRequest;
import com.example.dockb.entity.User;
import com.example.dockb.mapper.UserMapper;
import com.example.dockb.service.AuthService;
import com.example.dockb.util.JwtUtil;
import com.example.dockb.vo.AuthVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_USER = "USER";
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthVO register(RegisterRequest req) {
        // 检查用户名是否存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(ROLE_USER);
        userMapper.insert(user);
        log.info("[Auth] new user registered: id={}, username={}", user.getId(), user.getUsername());

        String token = jwtUtil.sign(user.getId(), user.getUsername(), user.getRole());
        return new AuthVO(user.getId(), user.getUsername(), user.getRole(), token);
    }

    @Override
    public AuthVO login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = jwtUtil.sign(user.getId(), user.getUsername(), user.getRole());
        log.info("[Auth] user logged in: id={}, username={}", user.getId(), user.getUsername());
        return new AuthVO(user.getId(), user.getUsername(), user.getRole(), token);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest req) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!encoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        user.setPassword(encoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
        log.info("[Auth] password changed: userId={}", userId);
    }
}
