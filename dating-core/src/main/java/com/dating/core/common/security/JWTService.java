package com.dating.core.common.security;


import com.dating.core.auth.domain.User;
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
     * @param user пользователь, чьи id и роль кладутся в claims
     * @return подписанный JWT
     */
    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Проверяет подпись и срок действия токена.
     *
     * @return {@code true}, если токен валиден
     */
    public boolean isValid(String token) {
        try{
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Извлекает id пользователя (subject) из валидного токена. */
    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    /** Извлекает роль из валидного токена. */
    public String extractRole(String token) {
        return parse(token).get("role", String.class);

    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
