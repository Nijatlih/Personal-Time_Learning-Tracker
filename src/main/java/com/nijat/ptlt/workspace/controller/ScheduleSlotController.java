package com.nijat.ptlt.workspace.controller;

import com.nijat.ptlt.auth.model.AuthenticatedUser;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.workspace.dto.ScheduleSlotCreateRequest;
import com.nijat.ptlt.workspace.dto.ScheduleSlotResponse;
import com.nijat.ptlt.workspace.dto.ScheduleSlotUpdateRequest;
import com.nijat.ptlt.workspace.service.ScheduleSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleSlotResponse>> create(
            @Valid @RequestBody ScheduleSlotCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        ScheduleSlotResponse response = scheduleSlotService.create(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleSlotResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<ScheduleSlotResponse> responses = scheduleSlotService.list(workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleSlotResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleSlotUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        ScheduleSlotResponse response = scheduleSlotService.update(id, request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        scheduleSlotService.delete(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
