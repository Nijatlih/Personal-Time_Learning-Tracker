package com.nijat.ptlt.sync.service;

import com.nijat.ptlt.common.exception.ConflictException;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotImplementedException;
import com.nijat.ptlt.sync.dto.SyncPullRequest;
import com.nijat.ptlt.sync.dto.SyncPushRequest;
import com.nijat.ptlt.sync.dto.SyncResponse;
import com.nijat.ptlt.workspace.entity.Workspace;
import com.nijat.ptlt.workspace.entity.WorkspaceMember;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import com.nijat.ptlt.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同步服务
 * Phase 0: 基础实现，仅支持Workspace和WorkspaceMember的同步
 * 
 * 同步策略：
 * - Pull: 返回 updatedAt > since 的所有实体
 * - Push: 校验version，冲突返回409
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    /**
     * Pull同步：获取自指定时间以来的所有变更
     */
    @Transactional(readOnly = true)
    public SyncResponse pull(SyncPullRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        OffsetDateTime since = parseSince(request.getSince());
        
        Map<String, Object> entities = new HashMap<>();
        
        // Pull workspaces（仅当前workspace）
        Optional<Workspace> workspaceOpt = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId);
        if (workspaceOpt.isPresent()) {
            Workspace workspace = workspaceOpt.get();
            if (workspace.getUpdatedAt().isAfter(since)) {
                entities.put("workspaces", List.of(workspaceToMap(workspace)));
            }
        }
        
        // Pull workspace_members
        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId);
        List<Map<String, Object>> memberMaps = members.stream()
                .filter(m -> m.getUpdatedAt().isAfter(since))
                .map(this::memberToMap)
                .collect(Collectors.toList());
        if (!memberMaps.isEmpty()) {
            entities.put("workspace_members", memberMaps);
        }
        
        String cursor = OffsetDateTime.now().format(ISO_FORMATTER);
        
        log.debug("Sync pull for workspace {}: {} entities", workspaceId, entities.size());
        return SyncResponse.builder()
                .entities(entities)
                .cursor(cursor)
                .build();
    }
    
    /**
     * Push同步：推送客户端变更
     * Phase 0: 仅做基础校验，完整实现在后续Phase
     */
    @Transactional
    public SyncResponse push(SyncPushRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);

        // 尚未实现。此前这里忽略整个 request，直接返回 pull() 的结果并包在
        // ApiResponse.success() 里 —— 离线客户端会收到 200 和一个新游标，据此认为
        // 队列已被接受并清空发件箱，而服务端其实什么都没写入。那是伪装成成功的数据丢失。
        // 在真正实现 version 冲突处理之前，必须明确地失败。
        log.warn("Sync push rejected for workspace {}: not implemented", workspaceId);
        throw new NotImplementedException(
                "Sync push is not implemented yet; client changes were NOT persisted");
    }
    
    /**
     * 校验用户对workspace的访问权限
     */
    private void validateWorkspaceAccess(UUID workspaceId, UUID userId) {
        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(
                workspaceId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this workspace");
        }
    }
    
    /**
     * 解析since时间戳
     */
    private OffsetDateTime parseSince(String since) {
        if (since == null || since.isEmpty()) {
            // 如果没有since，返回很久以前的时间
            return OffsetDateTime.now().minusYears(10);
        }
        try {
            return OffsetDateTime.parse(since, ISO_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid since format. Use ISO 8601 format.");
        }
    }
    
    /**
     * Workspace转换为Map（用于同步）
     */
    private Map<String, Object> workspaceToMap(Workspace workspace) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", workspace.getId().toString());
        map.put("name", workspace.getName());
        map.put("ownerId", workspace.getOwnerId().toString());
        map.put("createdAt", workspace.getCreatedAt().format(ISO_FORMATTER));
        map.put("updatedAt", workspace.getUpdatedAt().format(ISO_FORMATTER));
        map.put("version", workspace.getVersion());
        map.put("deletedAt", workspace.getDeletedAt() != null 
                ? workspace.getDeletedAt().format(ISO_FORMATTER) : null);
        return map;
    }
    
    /**
     * WorkspaceMember转换为Map（用于同步）
     */
    private Map<String, Object> memberToMap(WorkspaceMember member) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", member.getId().toString());
        map.put("workspaceId", member.getWorkspaceId().toString());
        map.put("userId", member.getUserId().toString());
        map.put("role", member.getRole().name());
        map.put("createdAt", member.getCreatedAt().format(ISO_FORMATTER));
        map.put("updatedAt", member.getUpdatedAt().format(ISO_FORMATTER));
        map.put("deletedAt", member.getDeletedAt() != null 
                ? member.getDeletedAt().format(ISO_FORMATTER) : null);
        return map;
    }
}
