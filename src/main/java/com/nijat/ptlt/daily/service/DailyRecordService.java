package com.nijat.ptlt.daily.service;

import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
import com.nijat.ptlt.daily.dto.DailyRecordCreateRequest;
import com.nijat.ptlt.daily.dto.DailyRecordResponse;
import com.nijat.ptlt.daily.dto.DailyRecordUpdateRequest;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import com.nijat.ptlt.daily.service.AlertService;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Daily Record服务
 * 所有方法必须校验workspace归属
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRecordService {
    private final DailyRecordRepository dailyRecordRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final StabilityService stabilityService;
    private final StreakService streakService;
    private final AlertService alertService;
    
    /**
     * 创建或更新daily record
     * 如果已存在该日期的记录，则更新；否则创建
     */
    @Transactional
    public DailyRecordResponse createOrUpdate(
            DailyRecordCreateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 查找是否已存在
        Optional<DailyRecord> existingOpt = dailyRecordRepository
                .findByWorkspaceIdAndDateAndDeletedAtIsNull(workspaceId, request.getDate());
        
        DailyRecord dailyRecord;
        if (existingOpt.isPresent()) {
            // 更新现有记录
            dailyRecord = existingOpt.get();
            updateDailyRecordFields(dailyRecord, request);
        } else {
            // 创建新记录
            dailyRecord = DailyRecord.builder()
                    .workspaceId(workspaceId)
                    .date(request.getDate())
                    .coreTaskCompleted(request.getCoreTaskCompleted())
                    .cognitiveEnergy(request.getCognitiveEnergy())
                    .alignedWithGoals(request.getAlignedWithGoals())
                    .build();
        }
        
        // 计算稳定度
        stabilityService.calculateAndSetStability(dailyRecord, workspaceId, request.getDate());
        
        // 计算streak
        streakService.calculateAndSetStreak(dailyRecord, workspaceId);
        
        dailyRecord = dailyRecordRepository.save(dailyRecord);
        
        // 检查并生成Gray Alert
        alertService.checkAndGenerateGrayAlert(workspaceId);
        
        log.info("Daily record saved: workspace={}, date={}, score={}",
                workspaceId, request.getDate(), dailyRecord.getStabilityScore());
        
        return toResponse(dailyRecord);
    }
    
    /**
     * 更新daily record
     */
    @Transactional
    public DailyRecordResponse update(
            UUID id, DailyRecordUpdateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        DailyRecord dailyRecord = dailyRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Daily record not found"));
        
        // 校验workspace归属和软删除
        if (dailyRecord.getDeletedAt() != null) {
            throw new NotFoundException("Daily record not found");
        }
        if (!dailyRecord.getWorkspaceId().equals(workspaceId)) {
            throw new ForbiddenException("Daily record does not belong to this workspace");
        }
        
        // 乐观锁校验。version 现在是 @NotNull，缺失会在进入本方法之前就被 @Valid 挡成 400，
        // 所以这里不再需要（也不该有）null 短路 —— 那等于允许客户端自己关掉冲突检测。
        if (!dailyRecord.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Daily record version mismatch", toResponse(dailyRecord));
        }
        
        // 更新字段
        if (request.getCoreTaskCompleted() != null) {
            dailyRecord.setCoreTaskCompleted(request.getCoreTaskCompleted());
        }
        if (request.getCognitiveEnergy() != null) {
            dailyRecord.setCognitiveEnergy(request.getCognitiveEnergy());
        }
        if (request.getAlignedWithGoals() != null) {
            dailyRecord.setAlignedWithGoals(request.getAlignedWithGoals());
        }
        
        // 重新计算稳定度
        stabilityService.calculateAndSetStability(dailyRecord, workspaceId, dailyRecord.getDate());
        
        // 重新计算streak
        streakService.calculateAndSetStreak(dailyRecord, workspaceId);
        
        dailyRecord = dailyRecordRepository.save(dailyRecord);
        
        // 检查并生成Gray Alert
        alertService.checkAndGenerateGrayAlert(workspaceId);
        
        log.info("Daily record updated: workspace={}, date={}", workspaceId, dailyRecord.getDate());
        return toResponse(dailyRecord);
    }
    
    /**
     * 获取指定日期的daily record
     */
    @Transactional(readOnly = true)
    public DailyRecordResponse getByDate(LocalDate date, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        DailyRecord dailyRecord = dailyRecordRepository
                .findByWorkspaceIdAndDateAndDeletedAtIsNull(workspaceId, date)
                .orElseThrow(() -> new NotFoundException("Daily record not found"));
        
        // 计算streak（实时计算）
        streakService.calculateAndSetStreak(dailyRecord, workspaceId);
        
        return toResponse(dailyRecord);
    }
    
    /**
     * 获取日期范围内的daily records
     */
    @Transactional(readOnly = true)
    public List<DailyRecordResponse> getByDateRange(
            LocalDate startDate, LocalDate endDate, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<DailyRecord> records = dailyRecordRepository
                .findByWorkspaceIdAndDateRange(workspaceId, startDate, endDate);
        
        // 为每条记录计算streak
        records.forEach(record -> streakService.calculateAndSetStreak(record, workspaceId));
        
        return records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新daily record字段（内部方法）
     */
    private void updateDailyRecordFields(DailyRecord dailyRecord, DailyRecordCreateRequest request) {
        if (request.getCoreTaskCompleted() != null) {
            dailyRecord.setCoreTaskCompleted(request.getCoreTaskCompleted());
        }
        if (request.getCognitiveEnergy() != null) {
            dailyRecord.setCognitiveEnergy(request.getCognitiveEnergy());
        }
        if (request.getAlignedWithGoals() != null) {
            dailyRecord.setAlignedWithGoals(request.getAlignedWithGoals());
        }
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
