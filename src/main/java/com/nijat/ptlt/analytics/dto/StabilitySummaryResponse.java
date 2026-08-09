package com.nijat.ptlt.analytics.dto;

import com.nijat.ptlt.daily.entity.StabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 稳定度摘要响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StabilitySummaryResponse {
    /**
     * 日期范围
     */
    private LocalDate startDate;
    private LocalDate endDate;
    
    /**
     * 平均稳定度评分
     */
    private Double averageScore;
    
    /**
     * 各状态的天数统计
     */
    private Map<StabilityStatus, Long> statusCounts;
    
    /**
     * 当前Green streak
     */
    private Integer currentGreenStreak;
    
    /**
     * 当前Uptime streak
     */
    private Integer currentUptimeStreak;
    
    /**
     * 最高Green streak（在日期范围内）
     */
    private Integer maxGreenStreak;
    
    /**
     * 最高Uptime streak（在日期范围内）
     */
    private Integer maxUptimeStreak;
}
