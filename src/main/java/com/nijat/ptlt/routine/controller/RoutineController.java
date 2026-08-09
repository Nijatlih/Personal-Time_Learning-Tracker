package com.nijat.ptlt.routine.controller;

import com.nijat.ptlt.auth.util.JwtUtil;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.routine.dto.RoutineCompletionRequest;
import com.nijat.ptlt.routine.dto.RoutineCompletionResponse;
import com.nijat.ptlt.routine.dto.RoutineDefinitionCreateRequest;
import com.nijat.ptlt.routine.dto.RoutineDefinitionResponse;
import com.nijat.ptlt.routine.dto.RoutineDefinitionUpdateRequest;
import com.nijat.ptlt.routine.service.RoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Routine Controller
 */
@RestController
@RequestMapping("/api/v1/routines")
@RequiredArgsConstructor
public class RoutineController {
    private final RoutineService routineService;
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
    
    // ========== Routine Definitions ==========
    
    @PostMapping("/definitions")
    public ResponseEntity<ApiResponse<RoutineDefinitionResponse>> createDefinition(
            @Valid @RequestBody RoutineDefinitionCreateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        RoutineDefinitionResponse response = routineService.createDefinition(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<List<RoutineDefinitionResponse>>> listDefinitions(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        List<RoutineDefinitionResponse> responses = routineService.listDefinitions(workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PutMapping("/definitions/{id}")
    public ResponseEntity<ApiResponse<RoutineDefinitionResponse>> updateDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody RoutineDefinitionUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        RoutineDefinitionResponse response = routineService.updateDefinition(id, request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ========== Routine Completions ==========
    
    @PostMapping("/completions")
    public ResponseEntity<ApiResponse<RoutineCompletionResponse>> recordCompletion(
            @Valid @RequestBody RoutineCompletionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = getUserIdFromHeader(authHeader);
        UUID workspaceId = getWorkspaceIdFromHeader(authHeader);
        RoutineCompletionResponse response = routineService.recordCompletion(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
