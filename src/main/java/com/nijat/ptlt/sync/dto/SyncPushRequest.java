package com.nijat.ptlt.sync.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Sync Push请求DTO
 * 包含客户端要推送的实体数据
 */
@Data
public class SyncPushRequest {
    /**
     * 实体类型 -> 实体数据列表
     * 例如: {"workspaces": [{...}], "workspace_members": [{...}]}
     */
    private Map<String, List<Map<String, Object>>> entities;
}
