package com.nijat.ptlt.daily.service;

import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.daily.dto.DashboardResponse;
import com.nijat.ptlt.daily.dto.DailyRecordResponse;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dashboard服务
 * Phase 1: 提供基础dashboard数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DailyRecordRepository dailyRecordRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final StreakService streakService;
    
    /**
     * 获取dashboard数据
     * 包含：当前稳定度、streak、最近记录
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 获取今天的记录
        LocalDate today = LocalDate.now();
        DailyRecord todayRecord = dailyRecordRepository
                .findByWorkspaceIdAndDateAndDeletedAtIsNull(workspaceId, today)
                .orElse(null);
        
        // 获取最近30天的记录
        LocalDate startDate = today.minusDays(29);
        List<DailyRecord> recentRecords = dailyRecordRepository
                .findByWorkspaceIdAndDateRange(workspaceId, startDate, today);
        
        // 为每条记录计算streak
        recentRecords.forEach(record -> streakService.calculateAndSetStreak(record, workspaceId));
        
        // 构建响应
        DashboardResponse.DashboardResponseBuilder builder = DashboardResponse.builder();
        
        if (todayRecord != null) {
            // 计算今天的streak
            streakService.calculateAndSetStreak(todayRecord, workspaceId);
            
            builder.currentStabilityScore(todayRecord.getStabilityScore())
                    .currentStabilityStatus(todayRecord.getStabilityStatus())
                    .greenStreak(todayRecord.getGreenStreak())
                    .uptimeStreak(todayRecord.getUptimeStreak());
        } else {
            // 如果没有今天的记录，从最近的记录获取streak
            if (!recentRecords.isEmpty()) {
                DailyRecord latest = recentRecords.get(0);
                builder.greenStreak(latest.getGreenStreak())
                        .uptimeStreak(latest.getUptimeStreak());
            }
        }
        
        // 转换为响应DTO
        List<DailyRecordResponse> recordResponses = recentRecords.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        builder.recentRecords(recordResponses);
        
        return builder.build();
    }
    
    /**
     * 校验用户对workspace的访问权限
     */
    private void validateWorkspaceAccess(UUID workspaceId, UUID userId) {
        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(
                workspaceId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this workspace");
        }
    }
    
    /**
     * 转换为响应DTO
     */
    private DailyRecordResponse toResponse(DailyRecord dailyRecord) {
        return DailyRecordResponse.builder()
                .id(dailyRecord.getId())
                .workspaceId(dailyRecord.getWorkspaceId())
                .date(dailyRecord.getDate())
                .stabilityScore(dailyRecord.getStabilityScore())
                .stabilityStatus(dailyRecord.getStabilityStatus())
                .morningRoutineCompleted(dailyRecord.getMorningRoutineCompleted())
                .eveningRoutineCompleted(dailyRecord.getEveningRoutineCompleted())
                .coreTaskCompleted(dailyRecord.getCoreTaskCompleted())
                .cognitiveEnergy(dailyRecord.getCognitiveEnergy())
                .alignedWithGoals(dailyRecord.getAlignedWithGoals())
                .greenStreak(dailyRecord.getGreenStreak())
                .uptimeStreak(dailyRecord.getUptimeStreak())
                .version(dailyRecord.getVersion())
                .build();
    }
}
