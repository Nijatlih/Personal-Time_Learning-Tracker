package com.nijat.ptlt.daily.dto;

import com.nijat.ptlt.daily.entity.StabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dashboard响应DTO
 * Phase 1: 提供基础dashboard数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    /**
     * 当前稳定度评分
     */
    private Integer currentStabilityScore;
    
    /**
     * 当前稳定度状态
     */
    private StabilityStatus currentStabilityStatus;
    
    /**
     * Green streak（连续GREEN天数）
     */
    private Integer greenStreak;
    
    /**
     * Uptime streak（连续非GRAY天数）
     */
    private Integer uptimeStreak;
    
    /**
     * 最近N天的记录（用于图表展示）
     */
    private List<DailyRecordResponse> recentRecords;
}
