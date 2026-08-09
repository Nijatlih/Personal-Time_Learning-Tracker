package com.nijat.ptlt.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Time Block响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlockResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID taskId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String title;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;
}
