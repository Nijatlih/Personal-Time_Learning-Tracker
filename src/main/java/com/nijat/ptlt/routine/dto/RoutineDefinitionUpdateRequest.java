package com.nijat.ptlt.routine.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新Routine定义请求DTO
 */
@Data
public class RoutineDefinitionUpdateRequest {
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private Integer version; // 乐观锁版本号
}
