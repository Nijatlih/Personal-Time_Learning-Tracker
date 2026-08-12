package com.nijat.ptlt.routine.service;

import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import com.nijat.ptlt.daily.service.StabilityService;
import com.nijat.ptlt.routine.dto.RoutineCompletionRequest;
import com.nijat.ptlt.routine.dto.RoutineCompletionResponse;
import com.nijat.ptlt.routine.dto.RoutineDefinitionCreateRequest;
import com.nijat.ptlt.routine.dto.RoutineDefinitionResponse;
import com.nijat.ptlt.routine.dto.RoutineDefinitionUpdateRequest;
import com.nijat.ptlt.routine.entity.RoutineCompletion;
import com.nijat.ptlt.routine.entity.RoutineDefinition;
import com.nijat.ptlt.routine.repository.RoutineCompletionRepository;
import com.nijat.ptlt.routine.repository.RoutineDefinitionRepository;
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
 * Routine服务
 * 所有方法必须校验workspace归属
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineService {
    private final RoutineDefinitionRepository routineDefinitionRepository;
    private final RoutineCompletionRepository routineCompletionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final StabilityService stabilityService;
    
    // ========== Routine Definition ==========
    
    /**
     * 创建routine定义
     */
    @Transactional
    public RoutineDefinitionResponse createDefinition(
            RoutineDefinitionCreateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 检查是否已存在同类型的routine定义
        Optional<RoutineDefinition> existingOpt = routineDefinitionRepository
                .findByWorkspaceIdAndTypeAndDeletedAtIsNull(workspaceId, request.getType());
        if (existingOpt.isPresent()) {
            throw new ConflictException("Routine definition of type " + request.getType() + " already exists");
        }
        
        RoutineDefinition definition = RoutineDefinition.builder()
                .workspaceId(workspaceId)
                .type(request.getType())
                .name(request.getName())
                .description(request.getDescription())
                .build();
        
        definition = routineDefinitionRepository.save(definition);
        
        log.info("Routine definition created: workspace={}, type={}", workspaceId, request.getType());
        return toDefinitionResponse(definition);
    }
    
    /**
     * 获取workspace的所有routine定义
     */
    @Transactional(readOnly = true)
    public List<RoutineDefinitionResponse> listDefinitions(UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<RoutineDefinition> definitions = routineDefinitionRepository
                .findByWorkspaceIdAndDeletedAtIsNull(workspaceId);
        
        return definitions.stream()
                .map(this::toDefinitionResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新routine定义
     */
    @Transactional
    public RoutineDefinitionResponse updateDefinition(
            UUID id, RoutineDefinitionUpdateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        RoutineDefinition definition = routineDefinitionRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Routine definition not found"));
        
        // 乐观锁校验。version 现在是 @NotNull，缺失会在进入本方法之前就被 @Valid 挡成 400，
        // 所以这里不再需要（也不该有）null 短路 —— 那等于允许客户端自己关掉冲突检测。
        if (!definition.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Routine definition version mismatch",
                    toDefinitionResponse(definition));
        }
        
        // 更新字段
        if (request.getName() != null) {
            definition.setName(request.getName());
        }
        if (request.getDescription() != null) {
            definition.setDescription(request.getDescription());
        }
        
        // saveAndFlush 而不是 save：@Version 只在 flush 时递增，而 save() 对一个已被管理的
        // 实体不会立刻 flush。不 flush 就构造响应，返回的 version 是**递增前**的旧值 ——
        // 客户端照着响应里的 version 发下一次更新，必然撞 409，连续编辑因此完全不可能
        // （version 现在是必填的，这个后果被放大了）。
        definition = routineDefinitionRepository.saveAndFlush(definition);
        
        log.info("Routine definition updated: workspace={}, id={}", workspaceId, id);
        return toDefinitionResponse(definition);
    }
    
    // ========== Routine Completion ==========
    
    /**
     * 记录routine完成情况
     */
    @Transactional
    public RoutineCompletionResponse recordCompletion(
            RoutineCompletionRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 校验routine定义存在且属于该workspace
        RoutineDefinition definition = routineDefinitionRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(request.getRoutineDefinitionId(), workspaceId)
                .orElseThrow(() -> new NotFoundException("Routine definition not found"));
        
        // 查找或创建完成记录
        Optional<RoutineCompletion> existingOpt = routineCompletionRepository
                .findByWorkspaceIdAndDateAndRoutineDefinitionIdAndDeletedAtIsNull(
                        workspaceId, request.getDate(), request.getRoutineDefinitionId());
        
        RoutineCompletion completion;
        if (existingOpt.isPresent()) {
            completion = existingOpt.get();
            completion.setCompleted(request.getCompleted());
        } else {
            completion = RoutineCompletion.builder()
                    .workspaceId(workspaceId)
                    .routineDefinitionId(request.getRoutineDefinitionId())
                    .date(request.getDate())
                    .completed(request.getCompleted())
                    .build();
        }
        
        completion = routineCompletionRepository.save(completion);
        
        // 如果存在该日期的daily record，重新计算稳定度
        Optional<DailyRecord> dailyRecordOpt = dailyRecordRepository
                .findByWorkspaceIdAndDateAndDeletedAtIsNull(workspaceId, request.getDate());
        if (dailyRecordOpt.isPresent()) {
            DailyRecord dailyRecord = dailyRecordOpt.get();
            stabilityService.calculateAndSetStability(dailyRecord, workspaceId, request.getDate());
            dailyRecordRepository.save(dailyRecord);
        }
        
        log.info("Routine completion recorded: workspace={}, date={}, completed={}",
                workspaceId, request.getDate(), request.getCompleted());
        
        return toCompletionResponse(completion);
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
     * 转换为定义响应DTO
     */
    private RoutineDefinitionResponse toDefinitionResponse(RoutineDefinition definition) {
        return RoutineDefinitionResponse.builder()
                .id(definition.getId())
                .workspaceId(definition.getWorkspaceId())
                .type(definition.getType())
                .name(definition.getName())
                .description(definition.getDescription())
                .createdAt(definition.getCreatedAt())
                .updatedAt(definition.getUpdatedAt())
                .version(definition.getVersion())
                .build();
    }
    
    /**
     * 转换为完成响应DTO
     */
    private RoutineCompletionResponse toCompletionResponse(RoutineCompletion completion) {
        return RoutineCompletionResponse.builder()
                .id(completion.getId())
                .workspaceId(completion.getWorkspaceId())
                .routineDefinitionId(completion.getRoutineDefinitionId())
                .date(completion.getDate())
                .completed(completion.getCompleted())
                .completedAt(completion.getCompletedAt())
                .version(completion.getVersion())
                .build();
    }
}
