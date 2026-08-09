package com.nijat.ptlt.daily.dto;

import com.nijat.ptlt.daily.entity.CognitiveEnergy;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建Daily Record请求DTO
 */
@Data
public class DailyRecordCreateRequest {
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    private Boolean coreTaskCompleted;
    
    private CognitiveEnergy cognitiveEnergy;
    
    private Boolean alignedWithGoals;
}
