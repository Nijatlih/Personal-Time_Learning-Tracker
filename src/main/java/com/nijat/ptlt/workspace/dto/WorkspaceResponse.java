package com.nijat.ptlt.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Workspace响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    private UUID id;
    private String name;
    private UUID ownerId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;
}
