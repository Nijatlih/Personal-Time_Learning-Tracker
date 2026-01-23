package com.nijat.ptlt.dto.auth;

public class UserDto {
  private String id;
  private String email;
  private String displayName;

  public UserDto() {}

  public UserDto(String id, String email, String displayName) {
    this.id = id;
    this.email = email;
    this.displayName = displayName;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
}
