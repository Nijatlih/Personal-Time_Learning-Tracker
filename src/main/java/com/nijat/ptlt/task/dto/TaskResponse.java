package com.nijat.ptlt.task.dto;

import com.nijat.ptlt.task.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private UUID id;
    private UUID workspaceId;
    private String title;
    private String description;
    private TaskStatus status;
    private Boolean isCoreTask;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;
}
