package com.nijat.ptlt.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Routine完成统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineCompletionStatsResponse {
    /**
     * 日期范围
     */
    private LocalDate startDate;
    private LocalDate endDate;
    
    /**
     * Morning routine完成率（0-100）
     */
    private Double morningCompletionRate;
    
    /**
     * Evening routine完成率（0-100）
     */
    private Double eveningCompletionRate;
    
    /**
     * Morning routine完成天数
     */
    private Long morningCompletedDays;
    
    /**
     * Evening routine完成天数
     */
    private Long eveningCompletedDays;
    
    /**
     * 总天数
     */
    private Long totalDays;
}
