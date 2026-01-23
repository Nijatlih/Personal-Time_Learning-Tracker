package com.nijat.ptlt.dto;

import java.time.Instant;

public class ApiError {
  private String code;
  private String message;
  private Instant timestamp = Instant.now();

  public ApiError() {}

  public ApiError(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
