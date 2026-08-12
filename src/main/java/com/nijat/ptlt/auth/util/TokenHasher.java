package com.nijat.ptlt.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * refresh token 的单向摘要，用于数据库存储。
 *
 * 此前 refresh_tokens.token 存的是**明文 token**。一次只读泄露 —— 备份文件、只读副本、
 * 一个能读表的 SQL 注入、误上传的 dump —— 就等于拿到全体用户未来 7 天的账号接管能力，
 * 因为这些字符串直接就能当凭据用。改存摘要之后，同样的泄露只能拿到一堆无法回放的哈希。
 *
 * 用 SHA-256 而不是 bcrypt：
 * - 这里防的是"离线拿到库之后能否直接回放"，而不是"能否暴力猜出原文"。
 *   refresh token 本身是 HMAC 签名过的 JWT，有 128 位以上的不可预测熵，
 *   不存在字典空间可言，所以慢哈希的加盐/慢速那套针对弱口令的性质在这里没有意义。
 * - 查找必须是按值精确命中（findByTokenHash）。bcrypt 每次加不同的盐，
 *   同一个 token 两次哈希结果不同，就只能全表扫着逐行 matches —— 既慢又荒谬。
 *
 * 输出 64 个字符的小写十六进制，与 token_hash VARCHAR(64) 对应。
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String sha256Hex(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 强制要求实现的算法，走不到这里
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
