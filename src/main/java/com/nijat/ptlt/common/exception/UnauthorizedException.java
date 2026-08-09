package com.nijat.ptlt.common.exception;

/**
 * 未授权异常（401）
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
