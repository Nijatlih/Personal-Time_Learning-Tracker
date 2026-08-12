package com.nijat.ptlt.task.controller;

import com.nijat.ptlt.auth.model.AuthenticatedUser;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.task.dto.TimeBlockCreateRequest;
import com.nijat.ptlt.task.dto.TimeBlockResponse;
import com.nijat.ptlt.task.dto.TimeBlockUpdateRequest;
import com.nijat.ptlt.task.service.TimeBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Time Block Controller
 */
@RestController
@RequestMapping("/api/v1/time-blocks")
@RequiredArgsConstructor
public class TimeBlockController {
    private final TimeBlockService timeBlockService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<TimeBlockResponse>> create(
            @Valid @RequestBody TimeBlockCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        TimeBlockResponse response = timeBlockService.create(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<TimeBlockResponse>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<TimeBlockResponse> responses = timeBlockService.getByDate(date, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<TimeBlockResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<TimeBlockResponse> responses = timeBlockService.getByDateRange(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TimeBlockResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        TimeBlockResponse response = timeBlockService.getById(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TimeBlockResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TimeBlockUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        TimeBlockResponse response = timeBlockService.update(id, request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        timeBlockService.delete(id, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
