package com.nijat.ptlt.common.exception;

/**
 * 版本冲突异常（409）
 * 用于乐观锁冲突场景
 */
public class ConflictException extends RuntimeException {
    private final Object serverState;
    
    public ConflictException(String message) {
        super(message);
        this.serverState = null;
    }
    
    public ConflictException(String message, Object serverState) {
        super(message);
        this.serverState = serverState;
    }
    
    public Object getServerState() {
        return serverState;
    }
}
