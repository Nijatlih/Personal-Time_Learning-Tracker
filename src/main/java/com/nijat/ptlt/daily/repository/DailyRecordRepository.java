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
     * 查找锚点日期当天及更早的记录，按日期倒序，最多 limit 条。
     *
     * 这是所有 streak / 连续 GRAY 计算的唯一入口，两个约束都是必要的：
     *
     * - `date <= :anchor`：连续天数必须从被计算的那条记录自己的日期往前数。
     *   如果把比锚点更新的记录也拿进来，向前走的循环在第一条就会因日期对不上而中断，
     *   算出来恒为 0。
     * - `LIMIT`：此前的版本没有上限，一次把 workspace 的全部历史记录load进内存，
     *   而调用方只会用到最前面连续的那一小段。
     */
    @Query(value = "SELECT * FROM daily_records WHERE workspace_id = :workspaceId " +
           "AND deleted_at IS NULL AND date <= :anchor " +
           "ORDER BY date DESC LIMIT :limit", nativeQuery = true)
    List<DailyRecord> findOnOrBeforeWithLimit(
            @Param("workspaceId") UUID workspaceId,
            @Param("anchor") LocalDate anchor,
            @Param("limit") int limit);
}
