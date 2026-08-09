package com.nijat.ptlt.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task Instance响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstanceResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID taskId;
    private LocalDate date;
    private Boolean completed;
    private OffsetDateTime completedAt;
    private Integer version;
}
