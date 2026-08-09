package com.nijat.ptlt.routine.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Routine完成请求DTO
 */
@Data
public class RoutineCompletionRequest {
    @NotNull(message = "Routine definition ID is required")
    private UUID routineDefinitionId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Completed is required")
    private Boolean completed;
}
