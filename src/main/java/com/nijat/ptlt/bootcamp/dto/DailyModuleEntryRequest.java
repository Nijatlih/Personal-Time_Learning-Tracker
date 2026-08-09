package com.nijat.ptlt.bootcamp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily Module Entry请求DTO
 */
@Data
public class DailyModuleEntryRequest {
    @NotNull(message = "Module ID is required")
    private UUID moduleId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Completed is required")
    private Boolean completed;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
