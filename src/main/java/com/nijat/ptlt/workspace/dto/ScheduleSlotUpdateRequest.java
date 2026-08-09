package com.nijat.ptlt.workspace.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 更新Schedule Slot请求DTO
 */
@Data
public class ScheduleSlotUpdateRequest {
    private DayOfWeek dayOfWeek;
    
    private LocalTime startTime;
    
    private LocalTime endTime;
    
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private Integer version; // 乐观锁版本号
}
