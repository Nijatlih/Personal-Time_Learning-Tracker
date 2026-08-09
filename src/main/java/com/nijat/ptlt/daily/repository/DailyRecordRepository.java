package com.nijat.ptlt.daily.repository;

import com.nijat.ptlt.daily.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Daily Record Repository
 */
@Repository
public interface DailyRecordRepository extends JpaRepository<DailyRecord, UUID> {
    /**
     * 根据workspace_id和date查找（唯一约束）
     */
    Optional<DailyRecord> findByWorkspaceIdAndDateAndDeletedAtIsNull(
            UUID workspaceId, LocalDate date);
    
    /**
     * 查找日期范围内的记录
     */
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.workspaceId = :workspaceId " +
           "AND dr.date BETWEEN :startDate AND :endDate " +
           "AND dr.deletedAt IS NULL " +
           "ORDER BY dr.date DESC")
    List<DailyRecord> findByWorkspaceIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * 查找最近的记录（用于计算streak）
     */
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.workspaceId = :workspaceId " +
           "AND dr.deletedAt IS NULL " +
           "ORDER BY dr.date DESC")
    List<DailyRecord> findRecentByWorkspaceId(@Param("workspaceId") UUID workspaceId);
    
    /**
     * 查找最近的N条记录
     */
    @Query(value = "SELECT * FROM daily_records WHERE workspace_id = :workspaceId " +
           "AND deleted_at IS NULL " +
           "ORDER BY date DESC LIMIT :limit", nativeQuery = true)
    List<DailyRecord> findRecentByWorkspaceIdWithLimit(
            @Param("workspaceId") UUID workspaceId,
            @Param("limit") int limit);
}
