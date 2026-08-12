package com.nijat.ptlt.daily.controller;

import com.nijat.ptlt.auth.model.AuthenticatedUser;
import com.nijat.ptlt.common.response.ApiResponse;
import com.nijat.ptlt.daily.dto.DailyRecordCreateRequest;
import com.nijat.ptlt.daily.dto.DailyRecordResponse;
import com.nijat.ptlt.daily.dto.DailyRecordUpdateRequest;
import com.nijat.ptlt.daily.service.DailyRecordService;
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
 * Daily Record Controller
 */
@RestController
@RequestMapping("/api/v1/daily-records")
@RequiredArgsConstructor
public class DailyRecordController {
    private final DailyRecordService dailyRecordService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<DailyRecordResponse>> create(
            @Valid @RequestBody DailyRecordCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        DailyRecordResponse response = dailyRecordService.createOrUpdate(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyRecordResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody DailyRecordUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        DailyRecordResponse response = dailyRecordService.update(id, request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<DailyRecordResponse>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        DailyRecordResponse response = dailyRecordService.getByDate(date, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<DailyRecordResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<DailyRecordResponse> responses = dailyRecordService.getByDateRange(
                startDate, endDate, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
