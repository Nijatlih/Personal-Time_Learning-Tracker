package com.nijat.ptlt.bootcamp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bootcamp模块响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootcampModuleResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private Integer orderIndex;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
