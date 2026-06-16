package com.example.dockb.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：签发与验证。
 */
@Slf4j
@Component
public class JwtUtil {

    private static final long ONE_DAY_MS = 86400_000L;

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret:doc-kb-secret-key-change-in-production-2026}") String secret,
                   @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        // HS256 requires at least 256 bits (32 bytes)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * 签发 Token。
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色
     */
    public String sign(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 验证并解析 Token。
     * @param token JWT 字符串
     * @return Claims，解析失败返回 null
     */
    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("[Jwt] parse failed: {}", e.getMessage());
            return null;
        }
    }

    public Long extractUserId(String token) {
        Claims c = parse(token);
        if (c == null) return null;
        try {
            return Long.parseLong(c.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        Claims c = parse(token);
        return c == null ? null : c.get("username", String.class);
    }

    public String extractRole(String token) {
        Claims c = parse(token);
        return c == null ? null : c.get("role", String.class);
    }
}
