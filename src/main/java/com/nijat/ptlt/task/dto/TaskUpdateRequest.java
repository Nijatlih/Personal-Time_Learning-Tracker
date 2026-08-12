package com.nijat.ptlt.task.dto;

import com.nijat.ptlt.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新Task请求DTO
 */
@Data
public class TaskUpdateRequest {
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private TaskStatus status;
    
    private Boolean isCoreTask;
    
    // 乐观锁版本号，必填。此前可以省略，省略时 service 直接跳过冲突检测 ——
    // 等于把并发写默认成“后写静默覆盖先写”，而不是报冲突。
    @NotNull(message = "Version is required")
    private Integer version;
}
