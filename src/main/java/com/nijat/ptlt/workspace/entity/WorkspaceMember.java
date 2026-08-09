package com.nijat.ptlt.workspace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Workspace成员实体
 * 管理用户与workspace的关联关系及权限
 */
@Entity
@Table(name = "workspace_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
