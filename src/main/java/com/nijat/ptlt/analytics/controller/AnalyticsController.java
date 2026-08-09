package com.nijat.ptlt.analytics.controller;

import com.nijat.ptlt.analytics.dto.RoutineCompletionStatsResponse;
import com.nijat.ptlt.analytics.dto.StabilitySummaryResponse;
import com.nijat.ptlt.analytics.dto.TaskCompletionStatsResponse;
import com.nijat.ptlt.analytics.service.AnalyticsService;
import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    private final JwtUtil jwtUtil;
    
    /**
     * 从请求头提取用户ID和workspace ID
     */
    private UUID getUserIdFromHeader(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
    }
    
    private UUID getWorkspaceIdFromHeader(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getWorkspaceIdFromToken(token);
    }
    
    @GetMapping("/stability/summary")
    public ResponseEntity<ApiResponse<StabilitySummaryResponse>> getStabilitySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        StabilitySummaryResponse response = analyticsService.getStabilitySummary(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/tasks/completion-stats")
    public ResponseEntity<ApiResponse<TaskCompletionStatsResponse>> getTaskCompletionStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        TaskCompletionStatsResponse response = analyticsService.getTaskCompletionStats(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/routines/completion-stats")
    public ResponseEntity<ApiResponse<RoutineCompletionStatsResponse>> getRoutineCompletionStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        RoutineCompletionStatsResponse response = analyticsService.getRoutineCompletionStats(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
