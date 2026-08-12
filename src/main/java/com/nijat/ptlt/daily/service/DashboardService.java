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
import java.util.ArrayList;
import java.util.Comparator;
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
        
        // 为每条记录计算streak。批量版本只查一次库 —— 逐条调用是个 N+1：
        // 30 条记录加今天那条就是 31 条各带 LIMIT 400 的查询（最多约 12000 行），
        // 而它们查的是同一个 workspace 的同一段历史，只是锚点不同。
        streakService.calculateAndSetStreaks(recentRecords, workspaceId);

        // 构建响应
        DashboardResponse.DashboardResponseBuilder builder = DashboardResponse.builder();

        // "当前 streak" 一律锚在今天，与 AnalyticsService 的 currentGreenStreak 口径一致。
        //
        // 此前没有今天的记录时，这里取 recentRecords.get(0)（最近一条）的 streak 当作
        // 当前值 —— 而在 streak 改成按记录自身日期锚定之后，那个值的含义是
        // "截至那一天的连击"。于是五天没录的人，Dashboard 会显示"连击 10 天"，
        // 把一个早已中断的连续段报成仍在进行中。没录今天，连续段就是断的。
        List<DailyRecord> streakBasis = new ArrayList<>(recentRecords);
        if (todayRecord != null && recentRecords.stream().noneMatch(r -> r.getDate().equals(today))) {
            streakBasis.add(todayRecord);
        }
        streakBasis.sort(Comparator.comparing(DailyRecord::getDate).reversed());

        builder.greenStreak(StreakService.countStreak(streakBasis, today, StreakService.IS_GREEN))
                .uptimeStreak(StreakService.countStreak(streakBasis, today, StreakService.IS_UP));

        if (todayRecord != null) {
            builder.currentStabilityScore(todayRecord.getStabilityScore())
                    .currentStabilityStatus(todayRecord.getStabilityStatus());
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
