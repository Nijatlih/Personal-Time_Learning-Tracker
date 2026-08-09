package com.nijat.ptlt.daily.dto;

import com.nijat.ptlt.daily.entity.CognitiveEnergy;
import lombok.Data;

/**
 * 更新Daily Record请求DTO
 */
@Data
public class DailyRecordUpdateRequest {
    private Boolean coreTaskCompleted;
    
    private CognitiveEnergy cognitiveEnergy;
    
    private Boolean alignedWithGoals;
    
    private Integer version; // 乐观锁版本号
}
