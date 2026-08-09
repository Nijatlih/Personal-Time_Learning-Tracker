package com.nijat.ptlt.routine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Routine定义实体
 * 定义workspace级别的routine模板（早晨/晚上）
 */
@Entity
@Table(name = "routine_definitions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutineType type; // MORNING 或 EVENING
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
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
