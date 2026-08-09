package com.nijat.ptlt.bootcamp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Daily Module Entry响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyModuleEntryResponse {
    private UUID id;
    private UUID workspaceId;
    private UUID moduleId;
    private LocalDate date;
    private Boolean completed;
    private String notes;
    private Integer version;
}
