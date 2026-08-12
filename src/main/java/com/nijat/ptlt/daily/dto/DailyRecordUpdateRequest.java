package com.nijat.ptlt.daily.dto;

import com.nijat.ptlt.daily.entity.CognitiveEnergy;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新Daily Record请求DTO
 */
@Data
public class DailyRecordUpdateRequest {
    private Boolean coreTaskCompleted;
    
    private CognitiveEnergy cognitiveEnergy;
    
    private Boolean alignedWithGoals;
    
    // 乐观锁版本号，必填。此前可以省略，省略时 service 直接跳过冲突检测 ——
    // 等于把并发写默认成“后写静默覆盖先写”，而不是报冲突。
    @NotNull(message = "Version is required")
    private Integer version;
}
