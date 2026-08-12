package com.nijat.ptlt.daily.controller;

import com.nijat.ptlt.auth.model.AuthenticatedUser;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.daily.dto.AlertResponse;
import com.nijat.ptlt.daily.entity.Alert;
import com.nijat.ptlt.daily.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Alert Controller
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlerts(
            @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<Alert> alerts = alertService.getAlerts(workspaceId, userId, unreadOnly);
        List<AlertResponse> responses = alerts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        alertService.markAsRead(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 转换为响应DTO
     */
    private AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .workspaceId(alert.getWorkspaceId())
                .type(alert.getType())
                .message(alert.getMessage())
                .isRead(alert.getIsRead())
                .readAt(alert.getReadAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
