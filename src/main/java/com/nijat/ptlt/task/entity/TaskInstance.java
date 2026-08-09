package com.nijat.ptlt.task.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task实例实体
 * 任务在特定日期的实例，用于跟踪完成情况
 */
@Entity
@Table(name = "task_instances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "task_id", "date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Column(name = "task_id", nullable = false)
    private UUID taskId;
    
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (completed && completedAt == null) {
            completedAt = OffsetDateTime.now();
        }
    }
}
