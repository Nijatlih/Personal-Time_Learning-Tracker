package com.nijat.ptlt.routine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Routine完成响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineCompletionResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID routineDefinitionId;
    private LocalDate date;
    private Boolean completed;
    private OffsetDateTime completedAt;
    private Integer version;
}
