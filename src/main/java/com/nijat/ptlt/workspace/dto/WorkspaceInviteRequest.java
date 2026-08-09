package com.nijat.ptlt.workspace.dto;

import com.nijat.ptlt.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 邀请成员请求DTO
 */
@Data
public class WorkspaceInviteRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotNull(message = "Role is required")
    private WorkspaceRole role;
}
