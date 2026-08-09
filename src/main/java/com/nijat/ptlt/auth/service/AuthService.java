package com.nijat.ptlt.auth.service;

import com.nijat.ptlt.auth.dto.AuthResponse;
import com.nijat.ptlt.auth.dto.LoginRequest;
import com.nijat.ptlt.auth.dto.RefreshRequest;
import com.nijat.ptlt.auth.dto.RegisterRequest;
import com.nijat.ptlt.auth.entity.RefreshToken;
import com.nijat.ptlt.auth.entity.User;
import com.nijat.ptlt.auth.repository.RefreshTokenRepository;
import com.nijat.ptlt.auth.repository.UserRepository;
import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.exception.UnauthorizedException;
import com.nijat.ptlt.common.exception.ValidationException;
import com.nijat.ptlt.workspace.entity.Workspace;
import com.nijat.ptlt.workspace.entity.WorkspaceMember;
import com.nijat.ptlt.workspace.entity.WorkspaceRole;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import com.nijat.ptlt.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * 注册新用户
     * 同时创建默认workspace并设置用户为OWNER
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查邮箱是否已存在
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }
        
        // 创建用户
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user = userRepository.save(user);
        
        // 创建默认workspace
        Workspace workspace = Workspace.builder()
                .name(request.getWorkspaceName())
                .ownerId(user.getId())
                .build();
        workspace = workspaceRepository.save(workspace);
        
        // 创建workspace成员关系（OWNER）
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(user.getId())
                .role(WorkspaceRole.OWNER)
                .build();
        workspaceMemberRepository.save(member);
        
        // 生成token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), workspace.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // 保存refresh token
        saveRefreshToken(user.getId(), refreshToken);
        
        log.info("User registered: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .workspaceId(workspace.getId())
                .build();
    }
    
    /**
     * 登录
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        
        // 获取用户的默认workspace（第一个OWNER权限的workspace）
        Workspace workspace = workspaceRepository.findFirstByOwnerId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("No workspace found for user"));
        
        // 生成token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), workspace.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // 保存refresh token
        saveRefreshToken(user.getId(), refreshToken);
        
        log.info("User logged in: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .workspaceId(workspace.getId())
                .build();
    }
    
    /**
     * 刷新token
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        // 验证refresh token
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        
        UUID userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());
        
        // 检查数据库中的token是否有效
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));
        
        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        
        // 撤销旧token
        refreshToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);
        
        // 获取用户的默认workspace
        Workspace workspace = workspaceRepository.findFirstByOwnerId(userId)
                .orElseThrow(() -> new UnauthorizedException("No workspace found for user"));
        
        // 生成新token
        String newAccessToken = jwtUtil.generateAccessToken(userId, workspace.getId());
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        
        // 保存新refresh token
        saveRefreshToken(userId, newRefreshToken);
        
        log.info("Token refreshed for user: {}", userId);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .workspaceId(workspace.getId())
                .build();
    }
    
    /**
     * 登出（撤销所有refresh token）
     */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, OffsetDateTime.now());
        log.info("User logged out: {}", userId);
    }
    
    /**
     * 保存refresh token到数据库
     */
    private void saveRefreshToken(UUID userId, String token) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}
