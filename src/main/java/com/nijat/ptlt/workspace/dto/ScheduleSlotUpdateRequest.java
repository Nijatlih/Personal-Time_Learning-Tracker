package com.nijat.ptlt.workspace.dto;

import jakarta.validation.constraints.NotNull;
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
    
    // 乐观锁版本号，必填。此前可以省略，省略时 service 直接跳过冲突检测 ——
    // 等于把并发写默认成“后写静默覆盖先写”，而不是报冲突。
    @NotNull(message = "Version is required")
    private Integer version;
}
