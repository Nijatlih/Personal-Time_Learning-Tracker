package com.nijat.ptlt.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建Workspace请求DTO
 */
@Data
public class WorkspaceCreateRequest {
    @NotBlank(message = "Workspace name is required")
    @Size(max = 100, message = "Workspace name must not exceed 100 characters")
    private String name;
}
