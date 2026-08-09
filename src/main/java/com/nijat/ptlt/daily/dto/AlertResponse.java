package com.nijat.ptlt.daily.dto;

import com.nijat.ptlt.daily.entity.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Alert响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private UUID id;
    private UUID workspaceId;
    private AlertType type;
    private String message;
    private Boolean isRead;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
}
