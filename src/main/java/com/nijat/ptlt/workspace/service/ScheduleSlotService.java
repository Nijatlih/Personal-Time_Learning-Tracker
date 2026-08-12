package com.nijat.ptlt.workspace.service;

import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
import com.nijat.ptlt.common.exception.ValidationException;
import com.nijat.ptlt.workspace.dto.ScheduleSlotCreateRequest;
import com.nijat.ptlt.workspace.dto.ScheduleSlotResponse;
import com.nijat.ptlt.workspace.dto.ScheduleSlotUpdateRequest;
import com.nijat.ptlt.workspace.entity.ScheduleSlot;
import com.nijat.ptlt.workspace.repository.ScheduleSlotRepository;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Schedule Slot服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSlotService {
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    
    /**
     * 创建schedule slot
     */
    @Transactional
    public ScheduleSlotResponse create(
            ScheduleSlotCreateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 校验时间范围
        if (request.getEndTime().isBefore(request.getStartTime()) ||
            request.getEndTime().equals(request.getStartTime())) {
            throw new ValidationException("End time must be after start time");
        }
        
        ScheduleSlot slot = ScheduleSlot.builder()
                .workspaceId(workspaceId)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .title(request.getTitle())
                .description(request.getDescription())
                .build();
        
        slot = scheduleSlotRepository.save(slot);
        
        log.info("Schedule slot created: workspace={}, id={}", workspaceId, slot.getId());
        return toResponse(slot);
    }
    
    /**
     * 获取workspace的所有schedule slots
     */
    @Transactional(readOnly = true)
    public List<ScheduleSlotResponse> list(UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<ScheduleSlot> slots = scheduleSlotRepository
                .findByWorkspaceIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(workspaceId);
        
        return slots.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新schedule slot
     */
    @Transactional
    public ScheduleSlotResponse update(
            UUID id, ScheduleSlotUpdateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        ScheduleSlot slot = scheduleSlotRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Schedule slot not found"));
        
        // 乐观锁校验。version 现在是 @NotNull，缺失会在进入本方法之前就被 @Valid 挡成 400，
        // 所以这里不再需要（也不该有）null 短路 —— 那等于允许客户端自己关掉冲突检测。
        if (!slot.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Schedule slot version mismatch", toResponse(slot));
        }
        
        // 更新字段
        if (request.getDayOfWeek() != null) {
            slot.setDayOfWeek(request.getDayOfWeek());
        }
        if (request.getStartTime() != null) {
            slot.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            slot.setEndTime(request.getEndTime());
        }
        if (request.getTitle() != null) {
            slot.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            slot.setDescription(request.getDescription());
        }
        
        // 校验时间范围
        if (slot.getEndTime().isBefore(slot.getStartTime()) ||
            slot.getEndTime().equals(slot.getStartTime())) {
            throw new ValidationException("End time must be after start time");
        }
        
        // saveAndFlush 而不是 save：@Version 只在 flush 时递增，而 save() 对一个已被管理的
        // 实体不会立刻 flush。不 flush 就构造响应，返回的 version 是**递增前**的旧值 ——
        // 客户端照着响应里的 version 发下一次更新，必然撞 409，连续编辑因此完全不可能
        // （version 现在是必填的，这个后果被放大了）。
        slot = scheduleSlotRepository.saveAndFlush(slot);
        
        log.info("Schedule slot updated: workspace={}, id={}", workspaceId, id);
        return toResponse(slot);
    }
    
    /**
     * 删除schedule slot（软删除）
     */
    @Transactional
    public void delete(UUID id, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        ScheduleSlot slot = scheduleSlotRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Schedule slot not found"));
        
        slot.setDeletedAt(java.time.OffsetDateTime.now());
        scheduleSlotRepository.save(slot);
        
        log.info("Schedule slot deleted: workspace={}, id={}", workspaceId, id);
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
    private ScheduleSlotResponse toResponse(ScheduleSlot slot) {
        return ScheduleSlotResponse.builder()
                .id(slot.getId())
                .workspaceId(slot.getWorkspaceId())
                .dayOfWeek(slot.getDayOfWeek())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .title(slot.getTitle())
                .description(slot.getDescription())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .version(slot.getVersion())
                .build();
    }
}
