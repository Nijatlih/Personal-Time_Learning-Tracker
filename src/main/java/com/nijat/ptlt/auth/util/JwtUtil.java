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
    /** Claim distinguishing an access token from a refresh token. */
    static final String CLAIM_TYPE = "type";
    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private static final int MIN_SECRET_BYTES = 32;

    /*
     * 两种 token 都带一个随机 jti。
     *
     * 没有它的时候，token 的全部内容就是 sub + type + iat + exp（access token 再多一个
     * workspaceId），而 JWT 的 iat/exp 只精确到秒 —— 同一个用户在同一秒内签发的两个 token
     * 是逐字节相同的字符串。refresh_tokens.token 上有 UNIQUE 约束，于是"登录之后立刻刷新"
     * 这条路径会直接撞唯一约束失败（此前表现为 500，补了 DataIntegrityViolation 处理器
     * 之后表现为 409）。jti 让每个 token 无条件唯一。
     */

    /**
     * Substrings of secrets that ship with the repository or with examples. This key is the root of
     * both authentication and tenant isolation — every controller derives userId and workspaceId
     * from it — so a publicly known value defeats the workspace checks entirely.
     */
    private static final String[] PLACEHOLDER_MARKERS = {
        "change-in-production", "change-me", "changeme", "replace-with", "please-change",
        "your-256-bit-secret", "secret-key-here"
    };

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {
        assertUsableSecret(secret);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /** Fails fast at startup rather than signing tokens with a guessable key. */
    static void assertUsableSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set; there is no default signing key");
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes, got " + length);
        }
        String lower = secret.toLowerCase();
        for (String marker : PLACEHOLDER_MARKERS) {
            if (lower.contains(marker)) {
                throw new IllegalStateException(
                        "JWT_SECRET must not be a known placeholder value (matched \"" + marker + "\")");
            }
        }
    }
    
    /**
     * 生成Access Token
     */
    public String generateAccessToken(UUID userId, UUID workspaceId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenValidity, ChronoUnit.MILLIS);
        
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("workspaceId", workspaceId.toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
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
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
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
     * 验证 Access Token。
     *
     * <p>必须断言 type claim。此前只校验签名与过期时间，因此 Refresh Token 也能当作
     * Access Token 使用：登出形同虚设，被撤销的 Refresh Token 在其 7 天有效期内仍可调用
     * 任何只需要 userId 的端点，包括 DELETE /api/v1/workspaces/{id}。
     */
    public boolean validateAccessToken(String token) {
        return hasType(token, TYPE_ACCESS);
    }

    /** 验证 Refresh Token，拒绝把 Access Token 当作 Refresh Token 使用。 */
    public boolean validateRefreshToken(String token) {
        return hasType(token, TYPE_REFRESH);
    }

    private boolean hasType(String token, String expectedType) {
        try {
            return expectedType.equals(parseToken(token).get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
