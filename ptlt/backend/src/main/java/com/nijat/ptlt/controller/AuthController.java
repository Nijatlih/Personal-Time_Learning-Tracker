package com.nijat.ptlt.controller;

import com.nijat.ptlt.dto.ApiResponse;
import com.nijat.ptlt.dto.auth.AuthResponse;
import com.nijat.ptlt.dto.auth.LoginRequest;
import com.nijat.ptlt.dto.auth.RegisterRequest;
import com.nijat.ptlt.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
    return ApiResponse.ok(authService.register(req));
  }

  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
    return ApiResponse.ok(authService.login(req));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    // Stateless JWT: client just discards tokens
    return ApiResponse.ok(null);
  }
}
