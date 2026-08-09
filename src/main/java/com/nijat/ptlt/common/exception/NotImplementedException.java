package com.nijat.ptlt.common.exception;

/**
 * 功能尚未实现异常（映射为 501 Not Implemented）。
 *
 * <p>用于那些已经暴露了路由、但业务逻辑还没写完的端点。返回 501 是为了让调用方明确知道
 * 请求没有被处理；返回 200 会让客户端误以为操作已成功。
 */
public class NotImplementedException extends RuntimeException {
    public NotImplementedException(String message) {
        super(message);
    }
}
