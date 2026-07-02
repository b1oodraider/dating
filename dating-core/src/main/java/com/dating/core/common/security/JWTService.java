package com.dating.core.common.security;


import com.dating.core.common.config.JWTProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Выпуск и валидация JWT access-токенов (алгоритм HS256).
 *
 * <p>Access-токен самодостаточен: его валидность проверяется по подписи
 * и сроку действия, без обращения к БД. Поэтому он короткоживущий.
 */

@Service
public class JWTService {
    private final SecretKey secretKey;

    private final Duration expiration;

    public JWTService(JWTProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(jwtProperties.accessTtlMinutes());
    }

    /**
     * Выпускает access-токен для пользователя.
     *
     * @param userId - id пользователя
     * @param role - роль пользователя
     * данные кладутся в claim
     * @return подписанный JWT
     */
    public String createAccessToken(UUID userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                // алгоритм фиксируем явно: без него jjwt выбирает HMAC по длине ключа
                // (ключ >= 48 байт даст HS384), а gateway проверяет строго HS256
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Проверяет подпись и срок действия токена и возвращает его claims.
     * Один вызов = одна проверка подписи; всё содержимое (subject, role)
     * дальше достаётся из возвращённых claims без повторного парсинга.
     *
     * @return claims валидного токена, {@link Optional#empty()} — если токен невалиден
     */
    public Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(parse(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
