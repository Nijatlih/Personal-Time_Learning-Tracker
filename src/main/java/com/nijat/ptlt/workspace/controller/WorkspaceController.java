package com.nijat.ptlt.workspace.controller;

import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.workspace.dto.WorkspaceCreateRequest;
import com.nijat.ptlt.workspace.dto.WorkspaceResponse;
import com.nijat.ptlt.workspace.dto.WorkspaceUpdateRequest;
import com.nijat.ptlt.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Workspace Controller
 * Phase 0: 基础CRUD和邀请功能
 */
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService workspaceService;
    private final JwtUtil jwtUtil;
    
    /**
     * 从请求头提取用户ID（临时实现，Phase 1将使用JWT过滤器）
     */
    private UUID getUserIdFromHeader(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @Valid @RequestBody WorkspaceCreateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        WorkspaceResponse response = workspaceService.create(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> list(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        List<WorkspaceResponse> workspaces = workspaceService.list(userId);
        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        WorkspaceResponse response = workspaceService.getById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkspaceUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        WorkspaceResponse response = workspaceService.update(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        workspaceService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
}
