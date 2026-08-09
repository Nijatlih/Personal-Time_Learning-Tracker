package com.nijat.ptlt.workspace.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新Workspace请求DTO
 */
@Data
public class WorkspaceUpdateRequest {
    @Size(max = 100, message = "Workspace name must not exceed 100 characters")
    private String name;
    
    private Integer version; // 乐观锁版本号
}
