package com.nijat.ptlt.task.dto;

import com.nijat.ptlt.task.entity.TaskStatus;
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
    
    private Integer version; // 乐观锁版本号
}
