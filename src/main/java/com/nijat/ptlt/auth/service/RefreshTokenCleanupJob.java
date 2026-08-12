package com.nijat.ptlt.auth.service;

import com.nijat.ptlt.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 定期清理过期的 refresh token。
 *
 * refresh_tokens 此前只增不减：每次登录和每次刷新都插一行，而
 * RefreshTokenRepository.deleteExpired 写好了却从来没有任何地方调用它。
 * 撤销走的是 UPDATE revoked_at，也不会删行。表因此随使用时长无限增长，
 * 而 token 一旦过期就再也不会被读到 —— 全是纯粹的死数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    /** 每天凌晨 3 点跑一次。删的是已过期的行，跑得早晚都不影响正确性。 */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        OffsetDateTime now = OffsetDateTime.now();
        refreshTokenRepository.deleteExpired(now);
        log.info("Expired refresh tokens purged (cutoff {})", now);
    }
}
