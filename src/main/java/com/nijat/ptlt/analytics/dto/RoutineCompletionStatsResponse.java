package com.nijat.ptlt.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Routine 完成统计。
 *
 * Morning 与 Evening 各自使用独立分母，因为两个定义可能在不同日期创建；
 * 定义创建之前的日期不属于“应完成但未完成”。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineCompletionStatsResponse {
    private LocalDate startDate;
    private LocalDate endDate;

    private Double morningCompletionRate;
    private Double eveningCompletionRate;

    private Long morningCompletedDays;
    private Long eveningCompletedDays;

    private Long morningTotalDays;
    private Long eveningTotalDays;
}
