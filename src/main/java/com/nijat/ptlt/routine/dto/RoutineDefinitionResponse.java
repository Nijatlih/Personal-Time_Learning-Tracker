package com.nijat.ptlt.routine.dto;

import com.nijat.ptlt.routine.entity.RoutineType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Routine定义响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineDefinitionResponse {
    private UUID id;
    private UUID workspaceId;
    private RoutineType type;
    private String name;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;
}
