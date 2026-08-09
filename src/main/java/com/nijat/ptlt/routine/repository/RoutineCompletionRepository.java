package com.nijat.ptlt.routine.repository;

import com.nijat.ptlt.routine.entity.RoutineCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Routine完成记录Repository
 */
@Repository
public interface RoutineCompletionRepository extends JpaRepository<RoutineCompletion, UUID> {
    /**
     * 根据workspace_id、date和routine_definition_id查找
     */
    Optional<RoutineCompletion> findByWorkspaceIdAndDateAndRoutineDefinitionIdAndDeletedAtIsNull(
            UUID workspaceId, LocalDate date, UUID routineDefinitionId);
    
    /**
     * 查找某一天的所有routine完成记录
     */
    List<RoutineCompletion> findByWorkspaceIdAndDateAndDeletedAtIsNull(
            UUID workspaceId, LocalDate date);
    
    /**
     * 查找某个routine定义在日期范围内的完成记录
     */
    @Query("SELECT rc FROM RoutineCompletion rc WHERE rc.workspaceId = :workspaceId " +
           "AND rc.routineDefinitionId = :routineDefinitionId " +
           "AND rc.date BETWEEN :startDate AND :endDate " +
           "AND rc.deletedAt IS NULL")
    List<RoutineCompletion> findByWorkspaceIdAndRoutineDefinitionIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("routineDefinitionId") UUID routineDefinitionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * 统计某个routine定义在日期范围内的完成数量
     */
    @Query("SELECT COUNT(rc) FROM RoutineCompletion rc WHERE rc.workspaceId = :workspaceId " +
           "AND rc.routineDefinitionId = :routineDefinitionId " +
           "AND rc.completed = true " +
           "AND rc.date BETWEEN :startDate AND :endDate " +
           "AND rc.deletedAt IS NULL")
    long countCompletedByWorkspaceIdAndRoutineDefinitionIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("routineDefinitionId") UUID routineDefinitionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
