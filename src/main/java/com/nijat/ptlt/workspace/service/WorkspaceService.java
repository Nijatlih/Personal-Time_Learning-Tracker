package com.nijat.ptlt.workspace.service;

import com.nijat.ptlt.auth.entity.User;
import com.nijat.ptlt.auth.repository.UserRepository;
import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
import com.nijat.ptlt.common.exception.ValidationException;
import com.nijat.ptlt.workspace.dto.WorkspaceCreateRequest;
import com.nijat.ptlt.workspace.dto.WorkspaceInviteRequest;
import com.nijat.ptlt.workspace.dto.WorkspaceResponse;
import com.nijat.ptlt.workspace.dto.WorkspaceUpdateRequest;
import com.nijat.ptlt.workspace.entity.Workspace;
import com.nijat.ptlt.workspace.entity.WorkspaceMember;
import com.nijat.ptlt.workspace.entity.WorkspaceRole;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import com.nijat.ptlt.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Workspace服务
 * 所有方法必须校验workspace归属和权限
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    
    /**
     * 创建workspace
     */
    @Transactional
    public WorkspaceResponse create(WorkspaceCreateRequest request, UUID ownerId) {
        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .ownerId(ownerId)
                .build();
        workspace = workspaceRepository.save(workspace);
        
        // 创建成员关系（OWNER）
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(ownerId)
                .role(WorkspaceRole.OWNER)
                .build();
        workspaceMemberRepository.save(member);
        
        log.info("Workspace created: {} by user: {}", workspace.getId(), ownerId);
        return toResponse(workspace);
    }
    
    /**
     * 获取workspace列表（当前用户）
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> list(UUID userId) {
        // 查找用户作为成员的所有workspace
        List<WorkspaceMember> members = workspaceMemberRepository.findByUserIdAndDeletedAtIsNull(userId);
        List<UUID> workspaceIds = members.stream()
                .map(WorkspaceMember::getWorkspaceId)
                .collect(Collectors.toList());
        
        return workspaceRepository.findAllById(workspaceIds).stream()
                .filter(w -> w.getDeletedAt() == null)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取workspace详情
     * 必须校验用户是workspace成员
     */
    @Transactional(readOnly = true)
    public WorkspaceResponse getById(UUID workspaceId, UUID userId) {
        // 校验成员身份
        validateMemberAccess(workspaceId, userId);
        
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        
        return toResponse(workspace);
    }
    
    /**
     * 更新workspace
     * 必须校验权限（OWNER或EDITOR）和版本号
     */
    @Transactional
    public WorkspaceResponse update(UUID workspaceId, WorkspaceUpdateRequest request, UUID userId) {
        // 校验成员身份和权限
        WorkspaceMember member = validateMemberAccess(workspaceId, userId);
        if (member.getRole() == WorkspaceRole.VIEWER) {
            throw new ForbiddenException("Viewer role cannot update workspace");
        }
        
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        
        // 乐观锁校验
        if (request.getVersion() != null && !workspace.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Workspace version mismatch", toResponse(workspace));
        }
        
        // 更新
        if (request.getName() != null) {
            workspace.setName(request.getName());
        }
        workspace = workspaceRepository.save(workspace);
        
        log.info("Workspace updated: {} by user: {}", workspaceId, userId);
        return toResponse(workspace);
    }
    
    /**
     * 删除workspace（软删除）
     * 必须校验权限（OWNER）
     */
    @Transactional
    public void delete(UUID workspaceId, UUID userId) {
        // 校验OWNER权限
        WorkspaceMember member = validateMemberAccess(workspaceId, userId);
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Only owner can delete workspace");
        }
        
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        
        java.time.OffsetDateTime deletedAt = java.time.OffsetDateTime.now();
        workspace.setDeletedAt(deletedAt);
        workspaceRepository.save(workspace);

        // 同事务内软删除成员行。此前只置 workspaces.deleted_at，成员行保持
        // deleted_at IS NULL，而所有数据服务只校验成员身份、从不检查 workspace 是否存活，
        // 因此用户重新登录后依然能访问"已删除"workspace 的全部数据 —— 删除形同装饰。
        List<WorkspaceMember> members =
                workspaceMemberRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId);
        for (WorkspaceMember m : members) {
            m.setDeletedAt(deletedAt);
        }
        workspaceMemberRepository.saveAll(members);

        log.info("Workspace deleted: {} by user: {} ({} memberships revoked)",
                workspaceId, userId, members.size());
    }
    
    /**
     * 邀请成员
     * 必须校验权限（OWNER或EDITOR）
     */
    @Transactional
    public void invite(UUID workspaceId, WorkspaceInviteRequest request, UUID inviterId) {
        // 校验成员身份和权限
        WorkspaceMember inviter = validateMemberAccess(workspaceId, inviterId);
        if (inviter.getRole() == WorkspaceRole.VIEWER) {
            throw new ForbiddenException("Viewer role cannot invite members");
        }

        // 只有 OWNER 能授予 OWNER。此前仅拦截 VIEWER，然后原样采用 request.getRole()：
        // EDITOR 可以邀请一个自己控制的账号为 OWNER，用它登录即可满足 delete 的 OWNER 校验，
        // 两个 HTTP 调用就能从 EDITOR 提权到删除整个 workspace。
        if (request.getRole() == WorkspaceRole.OWNER && inviter.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Only owner can grant the OWNER role");
        }

        // 查找被邀请用户
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        // 检查是否已是成员
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(workspaceId, user.getId())) {
            throw new ValidationException("User is already a member of this workspace");
        }
        
        // 创建成员关系
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(user.getId())
                .role(request.getRole())
                .build();
        workspaceMemberRepository.save(member);
        
        log.info("User {} invited to workspace {} with role {}", user.getId(), workspaceId, request.getRole());
    }
    
    /**
     * 校验用户是否是workspace成员
     * 返回成员信息，如果不存在或已删除则抛出异常
     */
    private WorkspaceMember validateMemberAccess(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserIdAndDeletedAtIsNull(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("User is not a member of this workspace"));
    }
    
    /**
     * 转换为响应DTO
     */
    private WorkspaceResponse toResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .ownerId(workspace.getOwnerId())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .version(workspace.getVersion())
                .build();
    }
}
