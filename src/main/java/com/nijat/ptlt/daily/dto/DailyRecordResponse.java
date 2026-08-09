package com.nijat.ptlt.daily.dto;

import com.nijat.ptlt.daily.entity.CognitiveEnergy;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily Record响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecordResponse {
    private UUID id;
    private UUID workspaceId;
    private LocalDate date;
    private Integer stabilityScore;
    private StabilityStatus stabilityStatus;
    private Boolean morningRoutineCompleted;
    private Boolean eveningRoutineCompleted;
    private Boolean coreTaskCompleted;
    private CognitiveEnergy cognitiveEnergy;
    private Boolean alignedWithGoals;
    private Integer greenStreak;
    private Integer uptimeStreak;
    private Integer version;
}
