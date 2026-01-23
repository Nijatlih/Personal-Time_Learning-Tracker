package com.nijat.ptlt.dto;

public class ApiResponse<T> {
  private boolean success;
  private T data;
  private ApiError error;

  public static <T> ApiResponse<T> ok(T data) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = true;
    r.data = data;
    return r;
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = false;
    r.error = new ApiError(code, message);
    return r;
  }

  public boolean isSuccess() { return success; }
  public T getData() { return data; }
  public ApiError getError() { return error; }

  public void setSuccess(boolean success) { this.success = success; }
  public void setData(T data) { this.data = data; }
  public void setError(ApiError error) { this.error = error; }

  public static class ApiError {
    private String code;
    private String message;

    public ApiError() {}
    public ApiError(String code, String message) { this.code = code; this.message = message; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public void setCode(String code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
  }
}
