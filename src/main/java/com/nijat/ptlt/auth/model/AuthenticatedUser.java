package com.nijat.ptlt.auth.model;

import java.util.UUID;

/**
 * 已认证请求的主体：来自 access token 的 userId 与 workspaceId。
 *
 * 由 {@code JwtAuthenticationFilter} 在验证 token 之后放进 SecurityContext，
 * controller 用 {@code @AuthenticationPrincipal AuthenticatedUser principal} 拿到。
 *
 * 存在的理由是消掉一段重复：此前 10 个 controller 各自写两个私有方法
 * （{@code getUserIdFromHeader} / {@code getWorkspaceIdFromHeader}），
 * 每个都做 {@code authHeader.replace("Bearer ", "")} 再解析一次 JWT ——
 * 于是同一个 token 在一次请求里被解析三次（filter 一次、controller 两次），
 * 而且认证语义散在 20 份几乎相同的代码里。
 * {@code AuthController.logout} 那条缺陷（F19：任何签名有效的 token 都能踢掉全部会话）
 * 正是这个模式的产物 —— 它漏掉了 filter 里那道 token 类型检查。
 *
 * 现在 token 只在 filter 里解析一次，类型校验也只有那一处。
 */
public record AuthenticatedUser(UUID userId, UUID workspaceId) {
}
