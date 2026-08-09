package com.nijat.ptlt.workspace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 创建Schedule Slot请求DTO
 */
@Data
public class ScheduleSlotCreateRequest {
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
