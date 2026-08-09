package com.nijat.ptlt.bootcamp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 每日模块记录实体
 * 记录某一天完成的bootcamp模块
 */
@Entity
@Table(name = "daily_module_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "date", "module_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyModuleEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Column(name = "module_id", nullable = false)
    private UUID moduleId;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "completed", nullable = false)
    private Boolean completed;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
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
    }
}
