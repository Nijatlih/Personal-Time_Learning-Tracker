package com.nijat.ptlt.task.controller;

import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.task.dto.TaskCreateRequest;
import com.nijat.ptlt.task.dto.TaskInstanceRequest;
import com.nijat.ptlt.task.dto.TaskInstanceResponse;
import com.nijat.ptlt.task.dto.TaskResponse;
import com.nijat.ptlt.task.dto.TaskUpdateRequest;
import com.nijat.ptlt.task.entity.TaskStatus;
import com.nijat.ptlt.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Task Controller
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
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
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody TaskCreateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        TaskResponse response = taskService.create(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        List<TaskResponse> responses = taskService.list(workspaceId, userId, status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        TaskResponse response = taskService.getById(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        TaskResponse response = taskService.update(id, request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> complete(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        TaskResponse response = taskService.complete(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<TaskResponse>> archive(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        TaskResponse response = taskService.archive(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ========== Task Instances ==========
    
    @PostMapping("/instances")
    public ResponseEntity<ApiResponse<TaskInstanceResponse>> recordInstance(
            @Valid @RequestBody TaskInstanceRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        TaskInstanceResponse response = taskService.recordInstance(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/instances/range")
    public ResponseEntity<ApiResponse<List<TaskInstanceResponse>>> getInstancesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        List<TaskInstanceResponse> responses = taskService.getInstancesByDateRange(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
