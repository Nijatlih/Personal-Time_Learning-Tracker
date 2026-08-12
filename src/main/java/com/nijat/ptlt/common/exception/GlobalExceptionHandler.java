package com.nijat.ptlt.common.exception;

import com.nijat.ptlt.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * 处理器的声明顺序在这里是有意义的：Spring 的 ExceptionHandlerExceptionResolver 排在
 * DefaultHandlerExceptionResolver 之前，一旦本类里某个 @ExceptionHandler 匹配上就停止分派。
 * 因此只要存在 @ExceptionHandler(Exception.class)，Spring 自带的那套 4xx 分类就永远轮不到 ——
 * 缺 Authorization header、坏 JSON、?startDate=abc、HTTP 方法不对，全部会变成 500。
 * 下面这些具体处理器就是把那些客户端错误重新归位；catch-all 保留在文件末尾兜真正的未知错误。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
    }
    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        Object details = e.getServerState() == null ? null : Map.of("serverState", e.getServerState());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONFLICT", e.getMessage(), details));
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbidden(ForbiddenException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", e.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(ValidationException e) {
        log.warn("Validation failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED", e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED", "Invalid input", errors));
    }
    
    /**
     * 格式错误的请求：缺必需的 header / 缺必需的查询参数 / 请求体不是合法 JSON /
     * 路径变量或查询参数类型不对（?startDate=abc）。全都是客户端的问题，不是 500。
     *
     * 消息只回“哪个东西错了”，不回异常本身的文本 —— 后者会把内部类型名和解析器细节
     * 漏给调用方。
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("Missing request header: {}", e.getHeaderName());
        return badRequest("Required header is missing: " + e.getHeaderName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(
            MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getParameterName());
        return badRequest("Required parameter is missing: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        log.warn("Parameter type mismatch: {}={}", e.getName(), e.getValue());
        return badRequest("Parameter has an invalid value: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return badRequest("Request body is missing or malformed");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("METHOD_NOT_ALLOWED",
                        "HTTP method not supported for this endpoint: " + e.getMethod()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException e) {
        log.warn("No handler for path: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("RESOURCE_NOT_FOUND", "No such endpoint"));
    }

    /**
     * 唯一约束冲突 → 409。
     *
     * 几个 upsert 都是 check-then-act（先 find，不存在再 insert）：TaskService.createInstance、
     * RoutineService.recordCompletion、DailyRecordService.createOrUpdate、
     * BootcampService.recordEntry。两个并发请求同时通过 find 这一步时，后写的那个会撞上
     * 唯一约束。这是并发冲突，语义上和 ConflictException 是一回事，不是服务端故障。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONFLICT",
                        "The resource was modified concurrently, or it already exists"));
    }

    /**
     * JPA 层的乐观锁冲突 → 409。
     *
     * service 里显式的 version 比较能挡住绝大多数情况，但两个请求在读到同一个 version
     * 之后才分别写入时，冲突要到 flush 才会暴露，那时抛的是这个异常而不是 ConflictException。
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException e) {
        log.warn("Optimistic locking failure: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONFLICT",
                        "The resource was modified by another request; reload and retry"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static ResponseEntity<ApiResponse<Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", message));
    }
}
