package com.nijat.ptlt.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Refresh Token 实体
 * 用于存储刷新令牌，支持轮换和撤销
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(nullable = false, unique = true, length = 500)
    private String token;
    
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
    
    /**
     * 检查token是否有效（未过期且未撤销）
     */
    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}
