package com.nijat.ptlt.dto.auth;

public class AuthResponse {
  private String accessToken;
  private String refreshToken;
  private long expiresIn;
  private UserDto user;
  private String workspaceId;

  public AuthResponse() {}

  public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserDto user, String workspaceId) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
    this.user = user;
    this.workspaceId = workspaceId;
  }

  public String getAccessToken() { return accessToken; }
  public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
  public String getRefreshToken() { return refreshToken; }
  public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
  public long getExpiresIn() { return expiresIn; }
  public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
  public UserDto getUser() { return user; }
  public void setUser(UserDto user) { this.user = user; }
  public String getWorkspaceId() { return workspaceId; }
  public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
}
