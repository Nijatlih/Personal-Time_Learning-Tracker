package com.nijat.ptlt.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类
 * 负责生成和解析JWT token
 */
@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }
    
    /**
     * 生成Access Token
     */
    public String generateAccessToken(UUID userId, UUID workspaceId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenValidity, ChronoUnit.MILLIS);
        
        return Jwts.builder()
                .subject(userId.toString())
                .claim("workspaceId", workspaceId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * 生成Refresh Token（仅包含userId）
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenValidity, ChronoUnit.MILLIS);
        
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * 解析token获取Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 从token中提取userId
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }
    
    /**
     * 从token中提取workspaceId
     */
    public UUID getWorkspaceIdFromToken(String token) {
        Claims claims = parseToken(token);
        String workspaceIdStr = claims.get("workspaceId", String.class);
        return workspaceIdStr != null ? UUID.fromString(workspaceIdStr) : null;
    }
    
    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
