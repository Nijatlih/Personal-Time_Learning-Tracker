package com.nijat.ptlt.service;

import com.nijat.ptlt.dto.auth.AuthResponse;
import com.nijat.ptlt.dto.auth.LoginRequest;
import com.nijat.ptlt.dto.auth.RegisterRequest;
import com.nijat.ptlt.dto.auth.UserDto;
import com.nijat.ptlt.entity.User;
import com.nijat.ptlt.entity.Workspace;
import com.nijat.ptlt.exception.ApiException;
import com.nijat.ptlt.repo.UserRepository;
import com.nijat.ptlt.repo.WorkspaceRepository;
import com.nijat.ptlt.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nijat.ptlt.entity.Workspace;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuthService {

  private static final long ACCESS_TOKEN_SECONDS = 60 * 60; // 1h
  private static final long REFRESH_TOKEN_SECONDS = 60L * 60 * 24 * 30; // 30d

  private final UserRepository userRepo;
  private final WorkspaceRepository workspaceRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RoutineService routineService;

  public AuthService(UserRepository userRepo,
                     WorkspaceRepository workspaceRepo,
                     PasswordEncoder passwordEncoder,
                     JwtService jwtService,
                     RoutineService routineService) {
    this.userRepo = userRepo;
    this.workspaceRepo = workspaceRepo;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.routineService = routineService;
  }

  @@ -60,32 +58,33 @@ public class AuthService {
    ws.setType(Workspace.Type.PERSONAL);
    ws = workspaceRepo.save(ws);

    // create default routine definitions for workspace
    routineService.ensureDefaults(ws);

    return issueTokens(user, ws);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    User user = userRepo.findByEmailIgnoreCase(req.getEmail().trim())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials"));

    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    Workspace ws = workspaceRepo.findByOwnerId(user.getId()).stream().findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_WORKSPACE", "Workspace missing"));

    return issueTokens(user, ws);
  }

  private AuthResponse issueTokens(User user, Workspace ws) {
    String access = jwtService.generateToken(user.getId().toString(), user.getEmail(), ACCESS_TOKEN_SECONDS);
    String refresh = jwtService.generateToken(user.getId().toString(), user.getEmail(), REFRESH_TOKEN_SECONDS);
    UUID userId = user.getId();
    String access = jwtService.generateAccessToken(userId, user.getEmail());
    String refresh = jwtService.generateRefreshToken(userId, user.getEmail());

    UserDto userDto = new UserDto(user.getId().toString(), user.getEmail(), user.getDisplayName());
    return new AuthResponse(access, refresh, ACCESS_TOKEN_SECONDS, userDto, ws.getId().toString());
  }
}