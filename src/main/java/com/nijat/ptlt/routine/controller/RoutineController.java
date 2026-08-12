package com.nijat.ptlt.routine.controller;

import com.nijat.ptlt.auth.model.AuthenticatedUser;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    
    // ========== Routine Definitions ==========
    
    @PostMapping("/definitions")
    public ResponseEntity<ApiResponse<RoutineDefinitionResponse>> createDefinition(
            @Valid @RequestBody RoutineDefinitionCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        RoutineDefinitionResponse response = routineService.createDefinition(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<List<RoutineDefinitionResponse>>> listDefinitions(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<RoutineDefinitionResponse> responses = routineService.listDefinitions(workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PutMapping("/definitions/{id}")
    public ResponseEntity<ApiResponse<RoutineDefinitionResponse>> updateDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody RoutineDefinitionUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        RoutineDefinitionResponse response = routineService.updateDefinition(id, request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ========== Routine Completions ==========
    
    @PostMapping("/completions")
    public ResponseEntity<ApiResponse<RoutineCompletionResponse>> recordCompletion(
            @Valid @RequestBody RoutineCompletionRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        RoutineCompletionResponse response = routineService.recordCompletion(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
