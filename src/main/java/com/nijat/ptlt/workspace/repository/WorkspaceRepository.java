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
     * 查找用户的第一个未删除workspace（用于登录时选择默认workspace）。
     *
     * <p>必须过滤 deleted_at：此前的 findFirstByOwnerId 会把已软删除的 workspace 也选中，
     * 于是用户重新登录后拿到的 JWT 仍指向"已删除"的 workspace。
     */
    Optional<Workspace> findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID ownerId);

}
