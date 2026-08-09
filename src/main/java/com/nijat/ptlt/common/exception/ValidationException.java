package com.nijat.ptlt.common.exception;

/**
 * 验证失败异常（400）
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
