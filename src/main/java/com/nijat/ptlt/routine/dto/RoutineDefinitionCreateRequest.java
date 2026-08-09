package com.nijat.ptlt.routine.dto;

import com.nijat.ptlt.routine.entity.RoutineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建Routine定义请求DTO
 */
@Data
public class RoutineDefinitionCreateRequest {
    @NotNull(message = "Type is required")
    private RoutineType type;
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
