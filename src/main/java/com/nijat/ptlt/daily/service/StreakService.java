package com.nijat.ptlt.daily.service;

import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Streak计算服务
 * 
 * 连续天数定义（按PRD-STRUCT）：
 * - Green streak: 连续GREEN天数
 * - Uptime streak: 连续status != GRAY天数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreakService {
    private final DailyRecordRepository dailyRecordRepository;
    
    /**
     * 计算并设置streak
     * 
     * 计算逻辑：
     * 1. 从最近的记录开始，向前查找连续天数
     * 2. Green streak: 连续GREEN天数
     * 3. Uptime streak: 连续非GRAY天数
     */
    @Transactional(readOnly = true)
    public void calculateAndSetStreak(DailyRecord dailyRecord, UUID workspaceId) {
        // 获取最近的记录（按日期倒序）
        List<DailyRecord> recentRecords = dailyRecordRepository.findRecentByWorkspaceId(workspaceId);
        
        // 计算Green streak
        int greenStreak = calculateGreenStreak(recentRecords);
        dailyRecord.setGreenStreak(greenStreak);
        
        // 计算Uptime streak
        int uptimeStreak = calculateUptimeStreak(recentRecords);
        dailyRecord.setUptimeStreak(uptimeStreak);
        
        log.debug("Streak calculated for workspace {}: green={}, uptime={}",
                workspaceId, greenStreak, uptimeStreak);
    }
    
    /**
     * 计算Green streak（连续GREEN天数）
     * 
     * 从最近的记录开始，向前查找连续的GREEN记录
     */
    private int calculateGreenStreak(List<DailyRecord> records) {
        int streak = 0;
        LocalDate expectedDate = LocalDate.now();
        
        for (DailyRecord record : records) {
            // 如果日期不连续，中断
            if (!record.getDate().equals(expectedDate)) {
                break;
            }
            
            // 如果状态不是GREEN，中断
            if (record.getStabilityStatus() != StabilityStatus.GREEN) {
                break;
            }
            
            streak++;
            expectedDate = expectedDate.minusDays(1);
        }
        
        return streak;
    }
    
    /**
     * 计算Uptime streak（连续非GRAY天数）
     * 
     * 从最近的记录开始，向前查找连续的非GRAY记录
     */
    private int calculateUptimeStreak(List<DailyRecord> records) {
        int streak = 0;
        LocalDate expectedDate = LocalDate.now();
        
        for (DailyRecord record : records) {
            // 如果日期不连续，中断
            if (!record.getDate().equals(expectedDate)) {
                break;
            }
            
            // 如果状态是GRAY，中断
            if (record.getStabilityStatus() == StabilityStatus.GRAY) {
                break;
            }
            
            streak++;
            expectedDate = expectedDate.minusDays(1);
        }
        
        return streak;
    }
}
