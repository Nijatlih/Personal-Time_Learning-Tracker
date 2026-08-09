package com.nijat.ptlt.task.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 更新Time Block请求DTO
 */
@Data
public class TimeBlockUpdateRequest {
    private UUID taskId;
    
    private LocalDate date;
    
    private LocalTime startTime;
    
    private LocalTime endTime;
    
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private Integer version; // 乐观锁版本号
}
