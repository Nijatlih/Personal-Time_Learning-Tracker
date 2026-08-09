package com.nijat.ptlt.sync.dto;

import lombok.Data;

/**
 * Sync Pull请求DTO
 */
@Data
public class SyncPullRequest {
    /**
     * 时间戳cursor（ISO 8601格式）
     * 返回 updatedAt > since 的所有实体
     */
    private String since;
}
