package com.nijat.ptlt.analytics.service;

import com.nijat.ptlt.analytics.dto.RoutineCompletionStatsResponse;
import com.nijat.ptlt.analytics.dto.StabilitySummaryResponse;
import com.nijat.ptlt.analytics.dto.TaskCompletionStatsResponse;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.util.DateRanges;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import com.nijat.ptlt.daily.service.StreakService;
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
import java.util.function.Predicate;
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
        DateRanges.validate(startDate, endDate);
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
        
        // 计算当前streak。这里的锚点是今天而不是记录自身的日期，是有意的：
        // "current streak" 的定义就是"截至今天连续了多少天"，没录今天就是 0。
        // 与 StreakService 按记录锚定的那套区分开 —— 那套算的是"那一天当时的连击"。
        LocalDate today = LocalDate.now();
        List<DailyRecord> recentRecords = dailyRecordRepository
                .findOnOrBeforeWithLimit(workspaceId, today, StreakService.STREAK_WINDOW_DAYS);

        int currentGreenStreak =
                StreakService.countStreak(recentRecords, today, StreakService.IS_GREEN);
        int currentUptimeStreak =
                StreakService.countStreak(recentRecords, today, StreakService.IS_UP);

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
        DateRanges.validate(startDate, endDate);
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 获取日期范围内的task instances
        List<TaskInstance> instances = taskInstanceRepository
                .findByWorkspaceIdAndDateRange(workspaceId, startDate, endDate);
        
        // 获取所有任务（只用来判断哪些是 core task）
        List<com.nijat.ptlt.task.entity.Task> tasks = taskRepository
                .findByWorkspaceIdAndDeletedAtIsNull(workspaceId);
        Set<UUID> coreTaskIds = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsCoreTask()))
                .map(com.nijat.ptlt.task.entity.Task::getId)
                .collect(Collectors.toSet());

        // 分子分母必须取自同一个区间。
        //
        // 分母此前用的是 workspace 的全量 Task（无日期过滤），而分子是区间内的 instance ——
        // 两者不同源。一个有 20 个任务定义的 workspace 查"昨天"这一天，即使当天安排的
        // 2 个任务全部完成，显示出来的也是 10% 而不是 100%。workspace 用得越久、
        // 查的区间越窄，这个数字越离谱。
        //
        // 正确的分母是"区间内实际有安排（有 instance）的 distinct 任务数"。
        Set<UUID> scheduledTaskIds = instances.stream()
                .map(TaskInstance::getTaskId)
                .collect(Collectors.toSet());
        Set<UUID> completedTaskIds = instances.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCompleted()))
                .map(TaskInstance::getTaskId)
                .collect(Collectors.toSet());

        long totalTasks = scheduledTaskIds.size();
        long completedTasks = completedTaskIds.size();

        double completionRate = totalTasks > 0
                ? (double) completedTasks / totalTasks * 100.0
                : 0.0;

        // 核心任务同理：分母是区间内有安排的核心任务数，不是 workspace 的全部核心任务
        long coreTasksTotal = scheduledTaskIds.stream().filter(coreTaskIds::contains).count();
        long coreTasksCompleted = completedTaskIds.stream().filter(coreTaskIds::contains).count();

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
        DateRanges.validate(startDate, endDate);
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 获取routine定义
        var morningRoutineOpt = routineDefinitionRepository
                .findByWorkspaceIdAndTypeAndDeletedAtIsNull(workspaceId, RoutineType.MORNING);
        var eveningRoutineOpt = routineDefinitionRepository
                .findByWorkspaceIdAndTypeAndDeletedAtIsNull(workspaceId, RoutineType.EVENING);
        
        // Morning 与 Evening 必须各自计算有效天数：两个定义可能不是同一天创建。
        // 定义创建之前没有“应该完成”的 routine，不能把那些日期算成未完成。
        LocalDate morningStart = morningRoutineOpt
                .map(routine -> laterOf(startDate, routine.getCreatedAt().toLocalDate()))
                .orElse(null);
        LocalDate eveningStart = eveningRoutineOpt
                .map(routine -> laterOf(startDate, routine.getCreatedAt().toLocalDate()))
                .orElse(null);

        long morningTotalDays = eligibleDays(morningStart, endDate);
        long eveningTotalDays = eligibleDays(eveningStart, endDate);

        long morningCompletedDays = 0;
        if (morningRoutineOpt.isPresent() && morningTotalDays > 0) {
            UUID morningRoutineId = morningRoutineOpt.get().getId();
            List<RoutineCompletion> morningCompletions = routineCompletionRepository
                    .findByWorkspaceIdAndRoutineDefinitionIdAndDateRange(
                            workspaceId, morningRoutineId, morningStart, endDate);
            morningCompletedDays = morningCompletions.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getCompleted()))
                    .count();
        }

        double morningCompletionRate = morningTotalDays > 0
                ? (double) morningCompletedDays / morningTotalDays * 100.0
                : 0.0;

        long eveningCompletedDays = 0;
        if (eveningRoutineOpt.isPresent() && eveningTotalDays > 0) {
            UUID eveningRoutineId = eveningRoutineOpt.get().getId();
            List<RoutineCompletion> eveningCompletions = routineCompletionRepository
                    .findByWorkspaceIdAndRoutineDefinitionIdAndDateRange(
                            workspaceId, eveningRoutineId, eveningStart, endDate);
            eveningCompletedDays = eveningCompletions.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getCompleted()))
                    .count();
        }

        double eveningCompletionRate = eveningTotalDays > 0
                ? (double) eveningCompletedDays / eveningTotalDays * 100.0
                : 0.0;

        return RoutineCompletionStatsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .morningCompletionRate(morningCompletionRate)
                .eveningCompletionRate(eveningCompletionRate)
                .morningCompletedDays(morningCompletedDays)
                .eveningCompletedDays(eveningCompletedDays)
                .morningTotalDays(morningTotalDays)
                .eveningTotalDays(eveningTotalDays)
                .build();
    }
    
    private LocalDate laterOf(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private long eligibleDays(LocalDate eligibleStart, LocalDate endDate) {
        if (eligibleStart == null || eligibleStart.isAfter(endDate)) {
            return 0;
        }
        return eligibleStart.datesUntil(endDate.plusDays(1)).count();
    }

    /**
     * 计算日期范围内的最高Green streak
     */
    private int calculateMaxGreenStreak(List<DailyRecord> records) {
        return calculateMaxStreak(records, StreakService.IS_GREEN);
    }

    /**
     * 计算日期范围内的最高Uptime streak
     */
    private int calculateMaxUptimeStreak(List<DailyRecord> records) {
        return calculateMaxStreak(records, StreakService.IS_UP);
    }

    /**
     * 在给定区间内找最长的连续段。与 StreakService.countStreak 的区别是方向：
     * 那个从一个锚点往前数一段，这个要遍历整个区间找最长的一段，所以按日期升序扫。
     *
     * 入参列表先拷贝再排序 —— 此前直接 records.sort(...) 会把调用方传进来的列表
     * 原地从倒序改成升序。
     */
    private int calculateMaxStreak(List<DailyRecord> records, Predicate<DailyRecord> qualifies) {
        if (records.isEmpty()) {
            return 0;
        }

        List<DailyRecord> ascending = new ArrayList<>(records);
        ascending.sort(Comparator.comparing(DailyRecord::getDate));

        int maxStreak = 0;
        int currentStreak = 0;
        LocalDate lastDate = null;

        for (DailyRecord record : ascending) {
            if (qualifies.test(record)) {
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
