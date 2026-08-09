package com.nijat.ptlt.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Sync响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponse {
    /**
     * 实体类型 -> 实体数据列表
     */
    private Map<String, Object> entities;
    
    /**
     * 下次pull的cursor（当前时间戳）
     */
    private String cursor;
}
