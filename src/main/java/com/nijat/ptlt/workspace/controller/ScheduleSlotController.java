package com.nijat.ptlt.workspace.controller;

import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.workspace.dto.ScheduleSlotCreateRequest;
import com.nijat.ptlt.workspace.dto.ScheduleSlotResponse;
import com.nijat.ptlt.workspace.dto.ScheduleSlotUpdateRequest;
import com.nijat.ptlt.workspace.service.ScheduleSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Schedule Slot Controller
 */
@RestController
@RequestMapping("/api/v1/schedule-slots")
@RequiredArgsConstructor
public class ScheduleSlotController {
    private final ScheduleSlotService scheduleSlotService;
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
    
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleSlotResponse>> create(
            @Valid @RequestBody ScheduleSlotCreateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        ScheduleSlotResponse response = scheduleSlotService.create(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleSlotResponse>>> list(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        List<ScheduleSlotResponse> responses = scheduleSlotService.list(workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleSlotResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleSlotUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        ScheduleSlotResponse response = scheduleSlotService.update(id, request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        scheduleSlotService.delete(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
