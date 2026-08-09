package com.nijat.ptlt.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 任务完成统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCompletionStatsResponse {
    /**
     * 日期范围
     */
    private LocalDate startDate;
    private LocalDate endDate;
    
    /**
     * 总任务数
     */
    private Long totalTasks;
    
    /**
     * 已完成任务数
     */
    private Long completedTasks;
    
    /**
     * 完成率（0-100）
     */
    private Double completionRate;
    
    /**
     * 核心任务完成数
     */
    private Long coreTasksCompleted;
    
    /**
     * 核心任务总数
     */
    private Long coreTasksTotal;
    
    /**
     * 核心任务完成率（0-100）
     */
    private Double coreTaskCompletionRate;
    
    /**
     * 各任务的完成情况（taskId -> 完成天数）
     */
    private Map<UUID, Long> taskCompletionDays;
}
