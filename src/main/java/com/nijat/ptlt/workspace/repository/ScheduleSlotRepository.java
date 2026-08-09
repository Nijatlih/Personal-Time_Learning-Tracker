package com.nijat.ptlt.workspace.repository;

import com.nijat.ptlt.workspace.entity.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Schedule Slot Repository
 */
@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, UUID> {
    /**
     * 查找workspace的所有schedule slots
     */
    List<ScheduleSlot> findByWorkspaceIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(UUID workspaceId);
    
    /**
     * 查找特定星期几的slots
     */
    List<ScheduleSlot> findByWorkspaceIdAndDayOfWeekAndDeletedAtIsNullOrderByStartTimeAsc(
            UUID workspaceId, DayOfWeek dayOfWeek);
    
    /**
     * 根据ID和workspace_id查找（用于多租户隔离）
     */
    Optional<ScheduleSlot> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);
}
