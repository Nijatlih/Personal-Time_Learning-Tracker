package com.nijat.ptlt.routine.dto;

import jakarta.validation.constraints.NotNull;
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
    
    // 乐观锁版本号，必填。此前可以省略，省略时 service 直接跳过冲突检测 ——
    // 等于把并发写默认成“后写静默覆盖先写”，而不是报冲突。
    @NotNull(message = "Version is required")
    private Integer version;
}
