package com.nijat.ptlt.workspace.controller;

import com.nijat.ptlt.auth.model.AuthenticatedUser;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.workspace.dto.WorkspaceResponse;
import com.nijat.ptlt.workspace.dto.WorkspaceUpdateRequest;
import com.nijat.ptlt.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    
    // 没有 create 端点。workspace 只在注册时创建一个，见 adr/0005。

    /** 返回 token 指向的那一个 workspace（数组长度恒为 1）。 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<WorkspaceResponse> workspaces = workspaceService.list(workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        WorkspaceResponse response = workspaceService.getById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkspaceUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        WorkspaceResponse response = workspaceService.update(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        workspaceService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
}
