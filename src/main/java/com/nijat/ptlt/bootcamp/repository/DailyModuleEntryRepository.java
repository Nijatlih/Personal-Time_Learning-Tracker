package com.nijat.ptlt.bootcamp.repository;

import com.nijat.ptlt.bootcamp.entity.DailyModuleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Daily Module Entry Repository
 */
@Repository
public interface DailyModuleEntryRepository extends JpaRepository<DailyModuleEntry, UUID> {
    /**
     * 根据workspace_id、date和module_id查找
     */
    Optional<DailyModuleEntry> findByWorkspaceIdAndDateAndModuleIdAndDeletedAtIsNull(
            UUID workspaceId, LocalDate date, UUID moduleId);
    
    /**
     * 查找某一天的所有模块记录
     */
    List<DailyModuleEntry> findByWorkspaceIdAndDateAndDeletedAtIsNull(
            UUID workspaceId, LocalDate date);
    
    /**
     * 查找日期范围内的模块记录
     */
    @Query("SELECT dme FROM DailyModuleEntry dme WHERE dme.workspaceId = :workspaceId " +
           "AND dme.date BETWEEN :startDate AND :endDate " +
           "AND dme.deletedAt IS NULL " +
           "ORDER BY dme.date DESC")
    List<DailyModuleEntry> findByWorkspaceIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
