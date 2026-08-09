package com.nijat.ptlt.analytics.service;

import com.nijat.ptlt.analytics.dto.RoutineCompletionStatsResponse;
import com.nijat.ptlt.analytics.dto.StabilitySummaryResponse;
import com.nijat.ptlt.analytics.dto.TaskCompletionStatsResponse;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import com.nijat.ptlt.routine.entity.RoutineCompletion;
import com.nijat.ptlt.routine.entity.RoutineType;
import com.nijat.ptlt.routine.repository.RoutineCompletionRepository;
import com.nijat.ptlt.routine.repository.RoutineDefinitionRepository;
import com.nijat.ptlt.task.entity.TaskInstance;
import com.nijat.ptlt.task.repository.TaskInstanceRepository;
import com.nijat.ptlt.task.repository.TaskRepository;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics服务
 * Phase 3: 规则驱动的分析功能
 * 所有分析都基于已有数据，使用确定性规则计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final DailyRecordRepository dailyRecordRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final TaskRepository taskRepository;
    private final RoutineCompletionRepository routineCompletionRepository;
    private final RoutineDefinitionRepository routineDefinitionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    
    /**
     * 获取稳定度摘要
     * 计算日期范围内的稳定度统计信息
     */
    @Transactional(readOnly = true)
    public StabilitySummaryResponse getStabilitySummary(
            LocalDate startDate, LocalDate endDate, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 获取日期范围内的daily records
        List<DailyRecord> records = dailyRecordRepository
                .findByWorkspaceIdAndDateRange(workspaceId, startDate, endDate);
        
        if (records.isEmpty()) {
            return StabilitySummaryResponse.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .averageScore(0.0)
                    .statusCounts(Map.of())
                    .build();
        }
        
        // 计算平均稳定度评分
        double averageScore = records.stream()
                .filter(r -> r.getStabilityScore() != null)
                .mapToInt(DailyRecord::getStabilityScore)
                .average()
                .orElse(0.0);
        
        // 统计各状态的天数
        Map<StabilityStatus, Long> statusCounts = records.stream()
                .filter(r -> r.getStabilityStatus() != null)
                .collect(Collectors.groupingBy(
                        DailyRecord::getStabilityStatus,
                        Collectors.counting()));
        
        // 计算当前streak（从最近的记录开始）
        List<DailyRecord> recentRecords = dailyRecordRepository
                .findRecentByWorkspaceId(workspaceId);
        
        int currentGreenStreak = calculateGreenStreak(recentRecords);
        int currentUptimeStreak = calculateUptimeStreak(recentRecords);
        
        // 计算日期范围内的最高streak
        int maxGreenStreak = calculateMaxGreenStreak(records);
        int maxUptimeStreak = calculateMaxUptimeStreak(records);
        
        return StabilitySummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .averageScore(averageScore)
                .statusCounts(statusCounts)
                .currentGreenStreak(currentGreenStreak)
                .currentUptimeStreak(currentUptimeStreak)
                .maxGreenStreak(maxGreenStreak)
                .maxUptimeStreak(maxUptimeStreak)
                .build();
    }
    
    /**
     * 获取任务完成统计
     */
    @Transactional(readOnly = true)
    public TaskCompletionStatsResponse getTaskCompletionStats(
            LocalDate startDate, LocalDate endDate, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 获取日期范围内的task instances
        List<TaskInstance> instances = taskInstanceRepository
                .findByWorkspaceIdAndDateRange(workspaceId, startDate, endDate);
        
        // 获取所有任务
        List<com.nijat.ptlt.task.entity.Task> tasks = taskRepository
                .findByWorkspaceIdAndDeletedAtIsNull(workspaceId);
        
        // 统计完成情况
        long completedTasks = instances.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCompleted()))
                .map(TaskInstance::getTaskId)
                .distinct()
                .count();
        
        long totalTasks = tasks.size();
        
        // 计算完成率
        double completionRate = totalTasks > 0 
                ? (double) completedTasks / totalTasks * 100.0 
                : 0.0;
        
        // 核心任务统计
        List<UUID> coreTaskIds = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsCoreTask()))
                .map(com.nijat.ptlt.task.entity.Task::getId)
                .collect(Collectors.toList());
        
        long coreTasksCompleted = instances.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCompleted()))
                .filter(i -> coreTaskIds.contains(i.getTaskId()))
                .map(TaskInstance::getTaskId)
                .distinct()
                .count();
        
        long coreTasksTotal = coreTaskIds.size();
        double coreTaskCompletionRate = coreTasksTotal > 0
                ? (double) coreTasksCompleted / coreTasksTotal * 100.0
                : 0.0;
        
        // 各任务的完成天数统计
        Map<UUID, Long> taskCompletionDays = instances.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCompleted()))
                .collect(Collectors.groupingBy(
                        TaskInstance::getTaskId,
                        Collectors.counting()));
        
        return TaskCompletionStatsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .completionRate(completionRate)
                .coreTasksCompleted(coreTasksCompleted)
                .coreTasksTotal(coreTasksTotal)
                .coreTaskCompletionRate(coreTaskCompletionRate)
                .taskCompletionDays(taskCompletionDays)
                .build();
    }
    
    /**
     * 获取Routine完成统计
     */
    @Transactional(readOnly = true)
    public RoutineCompletionStatsResponse getRoutineCompletionStats(
            LocalDate startDate, LocalDate endDate, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 获取routine定义
        var morningRoutineOpt = routineDefinitionRepository
                .findByWorkspaceIdAndTypeAndDeletedAtIsNull(workspaceId, RoutineType.MORNING);
        var eveningRoutineOpt = routineDefinitionRepository
                .findByWorkspaceIdAndTypeAndDeletedAtIsNull(workspaceId, RoutineType.EVENING);
        
        // 计算总天数
        long totalDays = startDate.datesUntil(endDate.plusDays(1)).count();
        
        // Morning routine统计
        long morningCompletedDays = 0;
        if (morningRoutineOpt.isPresent()) {
            UUID morningRoutineId = morningRoutineOpt.get().getId();
            List<RoutineCompletion> morningCompletions = routineCompletionRepository
                    .findByWorkspaceIdAndRoutineDefinitionIdAndDateRange(
                            workspaceId, morningRoutineId, startDate, endDate);
            morningCompletedDays = morningCompletions.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getCompleted()))
                    .count();
        }
        
        double morningCompletionRate = totalDays > 0
                ? (double) morningCompletedDays / totalDays * 100.0
                : 0.0;
        
        // Evening routine统计
        long eveningCompletedDays = 0;
        if (eveningRoutineOpt.isPresent()) {
            UUID eveningRoutineId = eveningRoutineOpt.get().getId();
            List<RoutineCompletion> eveningCompletions = routineCompletionRepository
                    .findByWorkspaceIdAndRoutineDefinitionIdAndDateRange(
                            workspaceId, eveningRoutineId, startDate, endDate);
            eveningCompletedDays = eveningCompletions.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getCompleted()))
                    .count();
        }
        
        double eveningCompletionRate = totalDays > 0
                ? (double) eveningCompletedDays / totalDays * 100.0
                : 0.0;
        
        return RoutineCompletionStatsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .morningCompletionRate(morningCompletionRate)
                .eveningCompletionRate(eveningCompletionRate)
                .morningCompletedDays(morningCompletedDays)
                .eveningCompletedDays(eveningCompletedDays)
                .totalDays(totalDays)
                .build();
    }
    
    /**
     * 计算Green streak
     */
    private int calculateGreenStreak(List<DailyRecord> records) {
        int streak = 0;
        LocalDate expectedDate = LocalDate.now();
        
        for (DailyRecord record : records) {
            if (!record.getDate().equals(expectedDate)) {
                break;
            }
            if (record.getStabilityStatus() != StabilityStatus.GREEN) {
                break;
            }
            streak++;
            expectedDate = expectedDate.minusDays(1);
        }
        
        return streak;
    }
    
    /**
     * 计算Uptime streak
     */
    private int calculateUptimeStreak(List<DailyRecord> records) {
        int streak = 0;
        LocalDate expectedDate = LocalDate.now();
        
        for (DailyRecord record : records) {
            if (!record.getDate().equals(expectedDate)) {
                break;
            }
            if (record.getStabilityStatus() == StabilityStatus.GRAY) {
                break;
            }
            streak++;
            expectedDate = expectedDate.minusDays(1);
        }
        
        return streak;
    }
    
    /**
     * 计算日期范围内的最高Green streak
     */
    private int calculateMaxGreenStreak(List<DailyRecord> records) {
        if (records.isEmpty()) {
            return 0;
        }
        
        // 按日期排序
        records.sort(Comparator.comparing(DailyRecord::getDate));
        
        int maxStreak = 0;
        int currentStreak = 0;
        LocalDate lastDate = null;
        
        for (DailyRecord record : records) {
            if (record.getStabilityStatus() == StabilityStatus.GREEN) {
                if (lastDate == null || record.getDate().equals(lastDate.plusDays(1))) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
            lastDate = record.getDate();
        }
        
        return maxStreak;
    }
    
    /**
     * 计算日期范围内的最高Uptime streak
     */
    private int calculateMaxUptimeStreak(List<DailyRecord> records) {
        if (records.isEmpty()) {
            return 0;
        }
        
        // 按日期排序
        records.sort(Comparator.comparing(DailyRecord::getDate));
        
        int maxStreak = 0;
        int currentStreak = 0;
        LocalDate lastDate = null;
        
        for (DailyRecord record : records) {
            if (record.getStabilityStatus() != StabilityStatus.GRAY) {
                if (lastDate == null || record.getDate().equals(lastDate.plusDays(1))) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
            lastDate = record.getDate();
        }
        
        return maxStreak;
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
}
