package com.example.dockb.config;

import com.example.dockb.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>从请求头 Authorization: Bearer <token> 提取并验证 JWT，
 * 成功后把 userId/username/role 放入 request attribute，供后续使用。
 * 验证失败或无 token 时放行（具体接口由 @PreAuthorize 控制）。
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "auth_userId";
    public static final String ATTR_USERNAME = "auth_username";
    public static final String ATTR_ROLE = "auth_role";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            Claims claims = jwtUtil.parse(token);
            if (claims != null) {
                Long userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                if (userId != null) {
                    request.setAttribute(ATTR_USER_ID, userId);
                    request.setAttribute(ATTR_USERNAME, username);
                    request.setAttribute(ATTR_ROLE, role);
                    log.debug("[Auth] JWT valid: userId={}, username={}, role={}", userId, username, role);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
