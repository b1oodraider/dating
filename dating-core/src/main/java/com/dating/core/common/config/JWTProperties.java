package com.dating.core.common.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Типобезопасные настройки JWT из префикса {@code app.jwt} в application.yml.
 *
 * @param secret            секрет для подписи HS256 (≥ 32 байта)
 * @param accessTtlMinutes  срок жизни access-токена в минутах
 * @param refreshTtlDays    срок жизни refresh-токена в днях
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JWTProperties(
        String secret,
        long accessTtlMinutes,
        long refreshTtlDays
) {
}
