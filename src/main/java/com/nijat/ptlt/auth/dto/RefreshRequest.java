package com.nijat.ptlt.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新Token请求DTO
 */
@Data
public class RefreshRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
