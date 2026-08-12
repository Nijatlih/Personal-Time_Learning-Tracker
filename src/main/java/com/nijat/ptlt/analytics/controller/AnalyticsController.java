package com.nijat.ptlt.analytics.controller;

import com.nijat.ptlt.analytics.dto.RoutineCompletionStatsResponse;
import com.nijat.ptlt.analytics.dto.StabilitySummaryResponse;
import com.nijat.ptlt.analytics.dto.TaskCompletionStatsResponse;
import com.nijat.ptlt.analytics.service.AnalyticsService;
import com.nijat.ptlt.auth.model.AuthenticatedUser;
import com.nijat.ptlt.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Analytics Controller
 * Phase 3: 规则驱动的分析功能
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    
    @GetMapping("/stability/summary")
    public ResponseEntity<ApiResponse<StabilitySummaryResponse>> getStabilitySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        StabilitySummaryResponse response = analyticsService.getStabilitySummary(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/tasks/completion-stats")
    public ResponseEntity<ApiResponse<TaskCompletionStatsResponse>> getTaskCompletionStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        TaskCompletionStatsResponse response = analyticsService.getTaskCompletionStats(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/routines/completion-stats")
    public ResponseEntity<ApiResponse<RoutineCompletionStatsResponse>> getRoutineCompletionStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        RoutineCompletionStatsResponse response = analyticsService.getRoutineCompletionStats(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
