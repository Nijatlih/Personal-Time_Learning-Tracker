package com.nijat.ptlt.task.repository;

import com.nijat.ptlt.task.entity.Task;
import com.nijat.ptlt.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Task Repository
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    /**
     * 根据workspace_id查找所有任务
     */
    List<Task> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
    
    /**
     * 根据workspace_id和状态查找任务
     */
    List<Task> findByWorkspaceIdAndStatusAndDeletedAtIsNull(UUID workspaceId, TaskStatus status);
    
    /**
     * 根据ID和workspace_id查找（用于多租户隔离）
     */
    Optional<Task> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);
    
    /**
     * 查找核心任务（用于稳定度计算）
     */
    List<Task> findByWorkspaceIdAndIsCoreTaskTrueAndDeletedAtIsNull(UUID workspaceId);
}
