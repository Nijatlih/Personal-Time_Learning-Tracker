package com.nijat.ptlt.sync.controller;

import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.sync.dto.SyncPullRequest;
import com.nijat.ptlt.sync.dto.SyncPushRequest;
import com.nijat.ptlt.sync.dto.SyncResponse;
import com.nijat.ptlt.sync.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 同步Controller
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {
    private final SyncService syncService;
    private final JwtUtil jwtUtil;
    
    /**
     * 从请求头提取用户ID和workspace ID（临时实现）
     */
    private UUID getUserIdFromHeader(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
    }
    
    private UUID getWorkspaceIdFromHeader(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getWorkspaceIdFromToken(token);
    }
    
    @PostMapping("/pull")
    public ResponseEntity<ApiResponse<SyncResponse>> pull(
            @RequestParam(required = false) String since,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        
        SyncPullRequest request = new SyncPullRequest();
        request.setSince(since);
        
        SyncResponse response = syncService.pull(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/push")
    public ResponseEntity<ApiResponse<SyncResponse>> push(
            @RequestBody SyncPushRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        
        SyncResponse response = syncService.push(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
