package com.nijat.ptlt.workspace.repository;

import com.nijat.ptlt.workspace.entity.WorkspaceMember;
import com.nijat.ptlt.workspace.entity.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Workspace Member Repository
 */
@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    /**
     * 根据workspace_id和user_id查找（用于权限校验）
     */
    Optional<WorkspaceMember> findByWorkspaceIdAndUserIdAndDeletedAtIsNull(
            UUID workspaceId, UUID userId);
    
    /**
     * 查找用户在workspace中的角色
     */
    @Query("SELECT wm.role FROM WorkspaceMember wm WHERE wm.workspaceId = :workspaceId AND wm.userId = :userId AND wm.deletedAt IS NULL")
    Optional<WorkspaceRole> findRoleByWorkspaceIdAndUserId(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);
    
    /**
     * 查找workspace的所有成员
     */
    List<WorkspaceMember> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
    
    /**
     * 查找用户所属的所有workspace
     */
    List<WorkspaceMember> findByUserIdAndDeletedAtIsNull(UUID userId);
    
    /**
     * 检查用户是否是workspace成员
     */
    boolean existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(UUID workspaceId, UUID userId);
}
