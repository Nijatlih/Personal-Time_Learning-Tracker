package com.nijat.ptlt.workspace.service;

import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
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

    /**
     * 获取当前 workspace。
     *
     * 只返回 token 指向的那一个，而不是用户名下的全部。
     *
     * 此前返回全部成员关系对应的 workspace —— 但 token 里的 workspaceId 固定取
     * "最早创建的那个"，且没有任何端点能切换（见 adr/0005）。于是这个列表会把
     * 用户点不进去的 workspace 也列出来：界面能看到它、能拿到它的 id，
     * 但所有数据端点都只认 token 里那一个。
     *
     * 返回类型保持 List 是为了不改动 API 形状（客户端拿到的仍是数组，只是长度恒为 1）。
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> list(UUID workspaceId, UUID userId) {
        validateMemberAccess(workspaceId, userId);

        return workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .map(this::toResponse)
                .map(List::of)
                .orElseGet(List::of);
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
     * 必须校验成员身份和版本号
     */
    @Transactional
    public WorkspaceResponse update(UUID workspaceId, WorkspaceUpdateRequest request, UUID userId) {
        // 校验成员身份。角色分支已随 invite 一起删除（见 adr/0003）：
        // 现在成员只可能是 OWNER，所以"是成员"和"有权限"是同一件事。
        validateMemberAccess(workspaceId, userId);

        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        
        // 乐观锁校验。version 现在是 @NotNull，缺失会在进入本方法之前就被 @Valid 挡成 400，
        // 所以这里不再需要（也不该有）null 短路 —— 那等于允许客户端自己关掉冲突检测。
        if (!workspace.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Workspace version mismatch", toResponse(workspace));
        }
        
        // 更新
        if (request.getName() != null) {
            workspace.setName(request.getName());
        }
        // saveAndFlush 而不是 save：@Version 只在 flush 时递增，而 save() 对一个已被管理的
        // 实体不会立刻 flush。不 flush 就构造响应，返回的 version 是**递增前**的旧值 ——
        // 客户端照着响应里的 version 发下一次更新，必然撞 409，连续编辑因此完全不可能
        // （version 现在是必填的，这个后果被放大了）。
        workspace = workspaceRepository.saveAndFlush(workspace);
        
        log.info("Workspace updated: {} by user: {}", workspaceId, userId);
        return toResponse(workspace);
    }
    
    /**
     * 删除workspace（软删除）
     * 必须校验成员身份
     */
    @Transactional
    public void delete(UUID workspaceId, UUID userId) {
        // 同上：成员只可能是 OWNER，成员身份校验已经等价于 OWNER 校验。
        validateMemberAccess(workspaceId, userId);

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
