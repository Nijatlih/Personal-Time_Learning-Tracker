package com.nijat.ptlt.daily.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 告警实体
 * 系统生成的告警，如Gray Alert
 */
@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;
    
    @Column(nullable = false, length = 500)
    private String message;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead;
    
    @Column(name = "read_at")
    private OffsetDateTime readAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        if (isRead == null) {
            isRead = false;
        }
    }
}
