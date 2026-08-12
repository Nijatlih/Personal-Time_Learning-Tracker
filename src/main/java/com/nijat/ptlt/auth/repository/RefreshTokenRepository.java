package com.nijat.ptlt.auth.repository;

import com.nijat.ptlt.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * 根据 token 的 SHA-256 摘要查找。调用方负责先做摘要 —— 明文 token 不进数据库。
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 撤销用户的所有token
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
    
    /**
     * 删除过期的token
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpired(@Param("now") OffsetDateTime now);
}
