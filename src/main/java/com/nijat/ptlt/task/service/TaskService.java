package com.nijat.ptlt.task.service;

import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import com.nijat.ptlt.daily.service.StabilityService;
import com.nijat.ptlt.task.dto.TaskCreateRequest;
import com.nijat.ptlt.task.dto.TaskInstanceRequest;
import com.nijat.ptlt.task.dto.TaskInstanceResponse;
import com.nijat.ptlt.task.dto.TaskResponse;
import com.nijat.ptlt.task.dto.TaskUpdateRequest;
import com.nijat.ptlt.task.entity.Task;
import com.nijat.ptlt.task.entity.TaskInstance;
import com.nijat.ptlt.task.entity.TaskStatus;
import com.nijat.ptlt.task.repository.TaskInstanceRepository;
import com.nijat.ptlt.task.repository.TaskRepository;
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
 * Task服务
 * 所有方法必须校验workspace归属
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final StabilityService stabilityService;
    
    // ========== Task CRUD ==========
    
    /**
     * 创建task
     */
    @Transactional
    public TaskResponse create(TaskCreateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        Task task = Task.builder()
                .workspaceId(workspaceId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .isCoreTask(request.getIsCoreTask() != null ? request.getIsCoreTask() : false)
                .build();
        
        task = taskRepository.save(task);
        
        log.info("Task created: workspace={}, id={}", workspaceId, task.getId());
        return toResponse(task);
    }
    
    /**
     * 获取workspace的所有tasks
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID workspaceId, UUID userId, TaskStatus status) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<Task> tasks;
        if (status != null) {
            tasks = taskRepository.findByWorkspaceIdAndStatusAndDeletedAtIsNull(workspaceId, status);
        } else {
            tasks = taskRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId);
        }
        
        return tasks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取task详情
     */
    @Transactional(readOnly = true)
    public TaskResponse getById(UUID id, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        Task task = taskRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        return toResponse(task);
    }
    
    /**
     * 更新task
     */
    @Transactional
    public TaskResponse update(UUID id, TaskUpdateRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        Task task = taskRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        // 乐观锁校验
        if (request.getVersion() != null && !task.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Task version mismatch", toResponse(task));
        }
        
        // 更新字段
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getIsCoreTask() != null) {
            task.setIsCoreTask(request.getIsCoreTask());
        }
        
        task = taskRepository.save(task);
        
        log.info("Task updated: workspace={}, id={}", workspaceId, id);
        return toResponse(task);
    }
    
    /**
     * 完成task
     */
    @Transactional
    public TaskResponse complete(UUID id, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        Task task = taskRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        task.setStatus(TaskStatus.COMPLETED);
        task = taskRepository.save(task);
        
        log.info("Task completed: workspace={}, id={}", workspaceId, id);
        return toResponse(task);
    }
    
    /**
     * 归档task
     */
    @Transactional
    public TaskResponse archive(UUID id, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        Task task = taskRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        task.setStatus(TaskStatus.ARCHIVED);
        task = taskRepository.save(task);
        
        log.info("Task archived: workspace={}, id={}", workspaceId, id);
        return toResponse(task);
    }
    
    // ========== Task Instance ==========
    
    /**
     * 记录task instance（某一天的任务完成情况）
     */
    @Transactional
    public TaskInstanceResponse recordInstance(
            TaskInstanceRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 校验task存在且属于该workspace
        Task task = taskRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(
                request.getTaskId(), workspaceId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        // 查找或创建instance
        Optional<TaskInstance> existingOpt = taskInstanceRepository
                .findByWorkspaceIdAndTaskIdAndDateAndDeletedAtIsNull(
                        workspaceId, request.getTaskId(), request.getDate());
        
        TaskInstance instance;
        if (existingOpt.isPresent()) {
            instance = existingOpt.get();
            instance.setCompleted(request.getCompleted());
        } else {
            instance = TaskInstance.builder()
                    .workspaceId(workspaceId)
                    .taskId(request.getTaskId())
                    .date(request.getDate())
                    .completed(request.getCompleted())
                    .build();
        }
        
        instance = taskInstanceRepository.save(instance);
        
        // 如果是核心任务且已完成，更新daily record的core_task_completed
        if (Boolean.TRUE.equals(task.getIsCoreTask()) && Boolean.TRUE.equals(request.getCompleted())) {
            Optional<DailyRecord> dailyRecordOpt = dailyRecordRepository
                    .findByWorkspaceIdAndDateAndDeletedAtIsNull(workspaceId, request.getDate());
            
            if (dailyRecordOpt.isPresent()) {
                DailyRecord dailyRecord = dailyRecordOpt.get();
                dailyRecord.setCoreTaskCompleted(true);
                // 重新计算稳定度
                stabilityService.calculateAndSetStability(dailyRecord, workspaceId, request.getDate());
                dailyRecordRepository.save(dailyRecord);
            }
        }
        
        log.info("Task instance recorded: workspace={}, task={}, date={}, completed={}",
                workspaceId, request.getTaskId(), request.getDate(), request.getCompleted());
        
        return toInstanceResponse(instance);
    }
    
    /**
     * 获取日期范围内的task instances
     */
    @Transactional(readOnly = true)
    public List<TaskInstanceResponse> getInstancesByDateRange(
            LocalDate startDate, LocalDate endDate, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<TaskInstance> instances = taskInstanceRepository
                .findByWorkspaceIdAndDateRange(workspaceId, startDate, endDate);
        
        return instances.stream()
                .map(this::toInstanceResponse)
                .collect(Collectors.toList());
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
    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .workspaceId(task.getWorkspaceId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .isCoreTask(task.getIsCoreTask())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion())
                .build();
    }
    
    /**
     * 转换为instance响应DTO
     */
    private TaskInstanceResponse toInstanceResponse(TaskInstance instance) {
        return TaskInstanceResponse.builder()
                .id(instance.getId())
                .workspaceId(instance.getWorkspaceId())
                .taskId(instance.getTaskId())
                .date(instance.getDate())
                .completed(instance.getCompleted())
                .completedAt(instance.getCompletedAt())
                .version(instance.getVersion())
                .build();
    }
}
