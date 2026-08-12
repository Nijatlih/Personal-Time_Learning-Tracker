package com.nijat.ptlt.auth.controller;

import com.nijat.ptlt.auth.dto.AuthResponse;
import com.nijat.ptlt.auth.dto.LoginRequest;
import com.nijat.ptlt.auth.dto.RefreshRequest;
import com.nijat.ptlt.auth.dto.RegisterRequest;
import com.nijat.ptlt.auth.service.AuthService;
import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.exception.UnauthorizedException;
import com.nijat.ptlt.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 认证Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 登出：撤销该用户全部 refresh token。
     *
     * 此前这里直接调 getUserIdFromToken，而它只 parseToken（验签名和过期），
     * 不验 type claim、不查 refresh_tokens.revoked_at。加上 /api/v1/auth/** 是
     * permitAll、filter 里那道类型检查也轮不到 —— 结果是任何签名有效且未过期的 token
     * （包括已经轮换掉、DB 里 revoked_at 已置位的 refresh token）都能把该用户的
     * 全部活跃会话踢掉，7 天内可反复触发，日志里还与正常登出无法区分。
     *
     * 现在要求必须是有效的 access token，并且用前缀校验而不是 replace ——
     * replace("Bearer ", "") 是全局替换，token 正文里出现这个子串就会被改坏，
     * 而且它还会接受根本没有 Bearer 前缀的 header。
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Authorization header must be a Bearer token");
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateAccessToken(token)) {
            throw new UnauthorizedException("Invalid or expired access token");
        }
        UUID userId = jwtUtil.getUserIdFromToken(token);
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
