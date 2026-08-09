package com.nijat.ptlt.daily.entity;

/**
 * 稳定度状态
 * 根据稳定度评分确定的状态
 */
public enum StabilityStatus {
    GREEN,   // ≥ 80
    YELLOW,  // 50-79
    GRAY     // < 50
}
