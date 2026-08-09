package com.nijat.ptlt.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Task Instance请求DTO
 */
@Data
public class TaskInstanceRequest {
    @NotNull(message = "Task ID is required")
    private UUID taskId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Completed is required")
    private Boolean completed;
}
