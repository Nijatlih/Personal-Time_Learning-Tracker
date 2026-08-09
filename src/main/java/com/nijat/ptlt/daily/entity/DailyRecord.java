package com.nijat.ptlt.daily.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 日常记录实体
 * 记录每天的状态和稳定度
 * 
 * 唯一约束：(workspace_id, date)
 */
@Entity
@Table(name = "daily_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Column(nullable = false)
    private LocalDate date;
    
    // 稳定度计算相关字段
    @Column(name = "stability_score")
    private Integer stabilityScore; // 0-100
    
    @Enumerated(EnumType.STRING)
    @Column(name = "stability_status")
    private StabilityStatus stabilityStatus; // GREEN, YELLOW, GRAY
    
    // 稳定度计算因子
    @Column(name = "morning_routine_completed")
    private Boolean morningRoutineCompleted;
    
    @Column(name = "evening_routine_completed")
    private Boolean eveningRoutineCompleted;
    
    @Column(name = "core_task_completed")
    private Boolean coreTaskCompleted;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "cognitive_energy")
    private CognitiveEnergy cognitiveEnergy;
    
    @Column(name = "aligned_with_goals")
    private Boolean alignedWithGoals;
    
    // 连续天数（计算字段，不持久化）
    @Transient
    private Integer greenStreak;
    
    @Transient
    private Integer uptimeStreak;
    
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
