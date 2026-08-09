package com.nijat.ptlt.task.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Time Block实体
 * 时间块，用于规划和管理时间段
 */
@Entity
@Table(name = "time_blocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Column(name = "task_id")
    private UUID taskId; // 关联的任务（可选）
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    
    @Column(length = 200)
    private String title; // 时间块标题
    
    @Column(length = 500)
    private String description;
    
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
