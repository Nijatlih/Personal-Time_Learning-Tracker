package com.nijat.ptlt.task.repository;

import com.nijat.ptlt.task.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Time Block Repository
 */
@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, UUID> {
    /**
     * 根据workspace_id和date查找某一天的所有time blocks
     */
    @Query("SELECT tb FROM TimeBlock tb WHERE tb.workspaceId = :workspaceId " +
           "AND tb.date = :date " +
           "AND tb.deletedAt IS NULL " +
           "ORDER BY tb.startTime ASC")
    List<TimeBlock> findByWorkspaceIdAndDateAndDeletedAtIsNull(
            @Param("workspaceId") UUID workspaceId,
            @Param("date") LocalDate date);
    
    /**
     * 查找日期范围内的time blocks
     */
    @Query("SELECT tb FROM TimeBlock tb WHERE tb.workspaceId = :workspaceId " +
           "AND tb.date BETWEEN :startDate AND :endDate " +
           "AND tb.deletedAt IS NULL " +
           "ORDER BY tb.date ASC, tb.startTime ASC")
    List<TimeBlock> findByWorkspaceIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * 根据ID和workspace_id查找（用于多租户隔离）
     */
    Optional<TimeBlock> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);
}
