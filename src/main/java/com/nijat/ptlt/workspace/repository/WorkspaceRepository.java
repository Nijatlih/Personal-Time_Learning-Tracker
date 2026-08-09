package com.nijat.ptlt.workspace.repository;

import com.nijat.ptlt.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Workspace Repository
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    /**
     * 根据ID和workspace_id查找（用于多租户隔离校验）
     */
    Optional<Workspace> findByIdAndDeletedAtIsNull(UUID id);
    
    /**
     * 查找用户拥有的workspace列表
     */
    List<Workspace> findByOwnerIdAndDeletedAtIsNull(UUID ownerId);
    
    /**
     * 查找用户的第一个workspace（用于默认workspace）
     */
    Optional<Workspace> findFirstByOwnerId(UUID ownerId);
    
    /**
     * 根据ID查找（包含已删除）
     */
    Optional<Workspace> findById(UUID id);
}
