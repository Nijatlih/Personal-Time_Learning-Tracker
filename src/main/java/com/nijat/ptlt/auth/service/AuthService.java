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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
    private final LoginRateLimiter loginRateLimiter;

    /** 与 JwtUtil 签发 refresh token 时用的是同一个配置项，保证 DB 行与 token 本身同期。 */
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    /**
     * 一个固定的 bcrypt 串，不对应任何真实密码。邮箱不存在时拿它去跑一次 matches，
     * 好让"邮箱不存在"和"密码错"两条路径的耗时相当。见 login()。
     */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

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
        
        // 只打 userId，不打原始 email —— 日志会被转发/归档到比数据库宽松得多的地方
        log.info("User registered: {}", user.getId());
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
        loginRateLimiter.checkNotLockedOut(request.getEmail());

        // 查找用户
        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail());

        // 验证密码。
        //
        // 邮箱不存在时也要跑一次 bcrypt，否则两条路径的耗时差得很远：邮箱不存在是
        // 单次索引查询后立刻返回（亚毫秒），邮箱存在但密码错则要多跑一次 cost 10 的
        // bcrypt（几十毫秒）。虽然两边返回的消息一样，但响应延迟本身就是一条
        // 账号枚举侧信道。DUMMY_HASH 是一个固定的、不对应任何真实密码的 bcrypt 串。
        String hash = userOpt.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), hash);

        if (userOpt.isEmpty() || !passwordMatches) {
            loginRateLimiter.recordFailure(request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
        User user = userOpt.get();

        // 获取用户的默认workspace（第一个OWNER权限的workspace）
        Workspace workspace = workspaceRepository.findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(user.getId())
                .orElseThrow(() -> new UnauthorizedException("No workspace found for user"));

        loginRateLimiter.recordSuccess(request.getEmail());

        // 生成token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), workspace.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 保存refresh token
        saveRefreshToken(user.getId(), refreshToken);

        // 只打 userId，不打原始 email —— 日志会被转发/归档到比数据库宽松得多的地方
        log.info("User logged in: {}", user.getId());
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
        if (!jwtUtil.validateRefreshToken(request.getRefreshToken())) {
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
        Workspace workspace = workspaceRepository.findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId)
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
     *
     * 有效期从 jwt.refresh-token-validity 读，不再写死 7 天。写死的时候，
     * DB 行的 expires_at 和 token 自己 exp claim 的来源是两个 —— 只要有人调过
     * 那个配置项，两者就不再一致：配得比 7 天短，token 已过期而 DB 还认为有效；
     * 配得比 7 天长，DB 先判过期而 token 本身还没到期。
     */
    private void saveRefreshToken(UUID userId, String token) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(refreshTokenValidity, ChronoUnit.MILLIS);
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}
