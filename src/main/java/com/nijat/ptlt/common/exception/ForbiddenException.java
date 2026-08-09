package com.nijat.ptlt.common.exception;

/**
 * 权限不足异常（403）
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
