package com.nijat.ptlt.routine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Routine完成记录实体
 * 记录某一天某个routine的完成情况
 */
@Entity
@Table(name = "routine_completions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "date", "routine_definition_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Column(name = "routine_definition_id", nullable = false)
    private UUID routineDefinitionId;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "completed", nullable = false)
    private Boolean completed;
    
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    @Version
    @Column(nullable = false)
    private Integer version;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (version == null) {
            version = 0;
        }
        if (completed == null) {
            completed = false;
        }
        // INSERT 不触发 @PreUpdate，所以这段必须在这里也写一份。此前只写在 @PreUpdate 里，
        // 结果最常见的路径 —— 第一次打卡就以 completed=true 落库 —— 会留下
        // completed=true 而 completed_at=NULL，只有第二次更新同一行才补得上。
        if (Boolean.TRUE.equals(completed) && completedAt == null) {
            completedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (Boolean.TRUE.equals(completed) && completedAt == null) {
            completedAt = OffsetDateTime.now();
        }
    }
}
