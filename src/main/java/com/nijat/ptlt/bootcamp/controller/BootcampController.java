package com.nijat.ptlt.bootcamp.controller;

import com.nijat.ptlt.auth.model.AuthenticatedUser;
import com.nijat.ptlt.bootcamp.dto.BootcampModuleResponse;
import com.nijat.ptlt.bootcamp.dto.DailyModuleEntryRequest;
import com.nijat.ptlt.bootcamp.dto.DailyModuleEntryResponse;
import com.nijat.ptlt.bootcamp.service.BootcampService;
import com.nijat.ptlt.common.response.ApiResponse;
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
 * Bootcamp Controller
 */
@RestController
@RequestMapping("/api/v1/bootcamp")
@RequiredArgsConstructor
public class BootcampController {
    private final BootcampService bootcampService;
    
    @GetMapping("/modules")
    public ResponseEntity<ApiResponse<List<BootcampModuleResponse>>> listModules() {
        List<BootcampModuleResponse> responses = bootcampService.listModules();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @PostMapping("/entries")
    public ResponseEntity<ApiResponse<DailyModuleEntryResponse>> recordEntry(
            @Valid @RequestBody DailyModuleEntryRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        DailyModuleEntryResponse response = bootcampService.recordEntry(request, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/entries/by-date")
    public ResponseEntity<ApiResponse<List<DailyModuleEntryResponse>>> getEntriesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        UUID workspaceId = principal.workspaceId();
        List<DailyModuleEntryResponse> responses = bootcampService.getEntriesByDate(
                date, workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
