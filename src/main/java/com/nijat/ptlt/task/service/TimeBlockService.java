package com.nijat.ptlt.task.service;

import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
import com.nijat.ptlt.common.exception.ValidationException;
import com.nijat.ptlt.task.dto.TimeBlockCreateRequest;
import com.nijat.ptlt.task.dto.TimeBlockResponse;
import com.nijat.ptlt.task.dto.TimeBlockUpdateRequest;
import com.nijat.ptlt.task.entity.TimeBlock;
import com.nijat.ptlt.task.repository.TaskRepository;
import com.nijat.ptlt.task.repository.TimeBlockRepository;
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
 * Time Block服务
 * 所有方法必须校验workspace归属
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeBlockService {
    private final TimeBlockRepository timeBlockRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskRepository taskRepository;

    /**
     * 创建time block
     */
    @Transactional
    public TimeBlockResponse create(TimeBlockCreateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);

        // 校验时间范围
        if (request.getEndTime().isBefore(request.getStartTime()) ||
            request.getEndTime().equals(request.getStartTime())) {
            throw new ValidationException("End time must be after start time");
        }

        validateTaskBelongsToWorkspace(request.getTaskId(), workspaceId);

        TimeBlock timeBlock = TimeBlock.builder()
                .workspaceId(workspaceId)
                .taskId(request.getTaskId())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .title(request.getTitle())
                .description(request.getDescription())
                .build();
        
        timeBlock = timeBlockRepository.save(timeBlock);
        
        log.info("Time block created: workspace={}, id={}", workspaceId, timeBlock.getId());
        return toResponse(timeBlock);
    }
    
    /**
     * 获取某一天的time blocks
     */
    @Transactional(readOnly = true)
    public List<TimeBlockResponse> getByDate(LocalDate date, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<TimeBlock> timeBlocks = timeBlockRepository
                .findByWorkspaceIdAndDateAndDeletedAtIsNull(workspaceId, date);
        
        return timeBlocks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取日期范围内的time blocks
     */
    @Transactional(readOnly = true)
    public List<TimeBlockResponse> getByDateRange(
            LocalDate startDate, LocalDate endDate, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<TimeBlock> timeBlocks = timeBlockRepository
                .findByWorkspaceIdAndDateRange(workspaceId, startDate, endDate);
        
        return timeBlocks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取time block详情
     */
    @Transactional(readOnly = true)
    public TimeBlockResponse getById(UUID id, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        TimeBlock timeBlock = timeBlockRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Time block not found"));
        
        return toResponse(timeBlock);
    }
    
    /**
     * 更新time block
     */
    @Transactional
    public TimeBlockResponse update(
            UUID id, TimeBlockUpdateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        TimeBlock timeBlock = timeBlockRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Time block not found"));
        
        // 乐观锁校验。version 现在是 @NotNull，缺失会在进入本方法之前就被 @Valid 挡成 400，
        // 所以这里不再需要（也不该有）null 短路 —— 那等于允许客户端自己关掉冲突检测。
        if (!timeBlock.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Time block version mismatch", toResponse(timeBlock));
        }
        
        // 更新字段
        if (request.getTaskId() != null) {
            validateTaskBelongsToWorkspace(request.getTaskId(), workspaceId);
            timeBlock.setTaskId(request.getTaskId());
        }
        if (request.getDate() != null) {
            timeBlock.setDate(request.getDate());
        }
        if (request.getStartTime() != null) {
            timeBlock.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            timeBlock.setEndTime(request.getEndTime());
        }
        if (request.getTitle() != null) {
            timeBlock.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            timeBlock.setDescription(request.getDescription());
        }
        
        // 校验时间范围
        if (timeBlock.getEndTime().isBefore(timeBlock.getStartTime()) ||
            timeBlock.getEndTime().equals(timeBlock.getStartTime())) {
            throw new ValidationException("End time must be after start time");
        }
        
        // saveAndFlush 而不是 save：@Version 只在 flush 时递增，而 save() 对一个已被管理的
        // 实体不会立刻 flush。不 flush 就构造响应，返回的 version 是**递增前**的旧值 ——
        // 客户端照着响应里的 version 发下一次更新，必然撞 409，连续编辑因此完全不可能
        // （version 现在是必填的，这个后果被放大了）。
        timeBlock = timeBlockRepository.saveAndFlush(timeBlock);
        
        log.info("Time block updated: workspace={}, id={}", workspaceId, id);
        return toResponse(timeBlock);
    }
    
    /**
     * 删除time block（软删除）
     */
    @Transactional
    public void delete(UUID id, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        TimeBlock timeBlock = timeBlockRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Time block not found"));
        
        timeBlock.setDeletedAt(java.time.OffsetDateTime.now());
        timeBlockRepository.save(timeBlock);
        
        log.info("Time block deleted: workspace={}, id={}", workspaceId, id);
    }
    
    /**
     * 校验 taskId 指向的 task 确实属于当前 workspace。
     *
     * 此前 create 和 update 都把 request.getTaskId() 原样写进实体，不做任何校验 ——
     * 这是全仓唯一一处真正的跨租户引用（IDOR）：调用方可以拿到别人 workspace 里某个
     * task 的 UUID，在自己的 workspace 里建一个 time block 指向它。数据库层面也拦不住，
     * time_blocks.task_id 的外键只约束"这个 task 存在"，不约束它属于谁。
     *
     * 后果不只是数据脏：TimeBlockResponse 会把这个 taskId 原样返回，等于确认了
     * "这个 UUID 在系统里存在"，成为一个存在性预言机。
     *
     * taskId 可空（time block 允许不关联任何 task），所以 null 直接放过。
     */
    private void validateTaskBelongsToWorkspace(UUID taskId, UUID workspaceId) {
        if (taskId == null) {
            return;
        }
        taskRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(taskId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
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
    private TimeBlockResponse toResponse(TimeBlock timeBlock) {
        return TimeBlockResponse.builder()
                .id(timeBlock.getId())
                .workspaceId(timeBlock.getWorkspaceId())
                .taskId(timeBlock.getTaskId())
                .date(timeBlock.getDate())
                .startTime(timeBlock.getStartTime())
                .endTime(timeBlock.getEndTime())
                .title(timeBlock.getTitle())
                .description(timeBlock.getDescription())
                .createdAt(timeBlock.getCreatedAt())
                .updatedAt(timeBlock.getUpdatedAt())
                .version(timeBlock.getVersion())
                .build();
    }
}
