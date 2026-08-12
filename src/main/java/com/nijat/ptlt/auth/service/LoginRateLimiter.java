package com.nijat.ptlt.auth.service;

import com.nijat.ptlt.common.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败次数限流。
 *
 * 此前 /api/v1/auth/login 完全没有限流、延迟或锁定，密码可以无限次猜，唯一的成本
 * 是单次 bcrypt（cost 10）。一个常见弱口令字典跑下来完全可行。
 *
 * 实现刻意保持简单：内存里的 ConcurrentHashMap，不引 bucket4j/resilience4j。
 * 这是单实例自用工具（见 adr/0001），进程重启即清空是可以接受的 ——
 * 目标是挡住持续的暴力猜解，不是做一套分布式配额。
 */
@Slf4j
@Component
public class LoginRateLimiter {

    /** 连续失败多少次后锁定。 */
    static final int MAX_FAILURES = 5;

    /** 锁定时长。 */
    static final Duration LOCKOUT = Duration.ofMinutes(15);

    /** 多久没有新的失败就把计数丢掉，避免 map 无限增长。 */
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);

    private final Map<String, Attempts> byKey = new ConcurrentHashMap<>();

    private record Attempts(int failures, Instant lastFailure) {
    }

    /**
     * 已被锁定则直接抛，不进入密码校验。
     *
     * 消息与密码错误时保持一致，不额外透露"这个邮箱存在且被锁了"。
     */
    public void checkNotLockedOut(String email) {
        Attempts attempts = byKey.get(key(email));
        if (attempts == null) {
            return;
        }
        boolean expired = attempts.lastFailure().plus(FAILURE_WINDOW).isBefore(Instant.now());
        if (expired) {
            byKey.remove(key(email));
            return;
        }
        if (attempts.failures() >= MAX_FAILURES
                && attempts.lastFailure().plus(LOCKOUT).isAfter(Instant.now())) {
            log.warn("Login temporarily locked out after {} failures", attempts.failures());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    public void recordFailure(String email) {
        byKey.compute(key(email), (k, prev) -> {
            Instant now = Instant.now();
            if (prev == null || prev.lastFailure().plus(FAILURE_WINDOW).isBefore(now)) {
                return new Attempts(1, now);
            }
            return new Attempts(prev.failures() + 1, now);
        });
    }

    public void recordSuccess(String email) {
        byKey.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
