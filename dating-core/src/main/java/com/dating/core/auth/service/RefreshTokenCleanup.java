package com.dating.core.auth.service;

import com.dating.core.auth.repo.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Периодическая чистка таблицы refresh-токенов.
 *
 * <p>Удаляются только просроченные токены. Отозванные (revoked) доживают
 * до своего expiry — они нужны для reuse-detection в {@link AuthService#refresh}.
 * Требует {@code @EnableScheduling} на конфигурации приложения.
 */
@Component
public class RefreshTokenCleanup {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Каждый день в 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        int removed = refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
        log.info("Purged {} expired refresh tokens", removed);
    }
}
