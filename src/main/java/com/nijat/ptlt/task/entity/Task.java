package com.nijat.ptlt.task.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task实体
 * 任务定义，可重复使用
 */
@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
    
    @Column(name = "is_core_task", nullable = false)
    private Boolean isCoreTask; // 是否为核心任务（用于稳定度计算）
    
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
        if (status == null) {
            status = TaskStatus.TODO;
        }
        if (isCoreTask == null) {
            isCoreTask = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
