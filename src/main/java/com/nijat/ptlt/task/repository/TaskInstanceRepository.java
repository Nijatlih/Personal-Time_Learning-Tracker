package com.nijat.ptlt.task.repository;

import com.nijat.ptlt.task.entity.TaskInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Task Instance Repository
 */
@Repository
public interface TaskInstanceRepository extends JpaRepository<TaskInstance, UUID> {
    /**
     * 根据workspace_id、task_id和date查找
     */
    Optional<TaskInstance> findByWorkspaceIdAndTaskIdAndDateAndDeletedAtIsNull(
            UUID workspaceId, UUID taskId, LocalDate date);
    
    /**
     * 查找日期范围内的task instances
     */
    @Query("SELECT ti FROM TaskInstance ti WHERE ti.workspaceId = :workspaceId " +
           "AND ti.date BETWEEN :startDate AND :endDate " +
           "AND ti.deletedAt IS NULL " +
           "ORDER BY ti.date DESC")
    List<TaskInstance> findByWorkspaceIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    
    /**
     * 查找某一天的所有task instances
     */
    List<TaskInstance> findByWorkspaceIdAndDateAndDeletedAtIsNull(UUID workspaceId, LocalDate date);
}
