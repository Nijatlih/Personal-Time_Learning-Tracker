package com.nijat.ptlt.auth.repository;

import com.nijat.ptlt.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * 根据邮箱查找用户（排除已删除）
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    
    /**
     * 检查邮箱是否存在（排除已删除）
     */
    boolean existsByEmailAndDeletedAtIsNull(String email);
}
