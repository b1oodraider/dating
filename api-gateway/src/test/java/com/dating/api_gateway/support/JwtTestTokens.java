package com.dating.api_gateway.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Фабрика тестовых JWT.
 *
 * Гейтвей валидирует токены как OAuth2 resource-server с симметричным ключом
 * (SecurityConfig: NimbusReactiveJwtDecoder.withSecretKey, HmacSHA256). Значит валидный
 * тестовый токен должен быть подписан HS256 тем же секретом. Захардкоженный чужой токен
 * (например, с jwt.io) пройти не может — у него другая подпись.
 *
 * Nimbus (com.nimbusds.*) доступен транзитивно из spring-security-oauth2-jose.
 */
public final class JwtTestTokens {

    private JwtTestTokens() {}

    /** Валидный HS256-токен: подпись сходится, exp в будущем. Проходит проверку гейтвея. */
    public static String validHs256(String secret) {
        return build(secret, Instant.now().plus(1, ChronoUnit.HOURS));
    }

    /** Просроченный токен: подпись верна, но exp в прошлом => гейтвей вернёт 401. */
    public static String expiredHs256(String secret) {
        return build(secret, Instant.now().minus(1, ChronoUnit.HOURS));
    }

    /**
     * Токен, подписанный ЧУЖИМ секретом. Структурно валиден, но подпись не сойдётся
     * с секретом гейтвея => 401. Имитирует подделку.
     */
    public static String wrongSignature() {
        // 32+ байта — HS256 требует ключ не короче размера хеша.
        return build("totally-different-secret-key-32-bytes-long!!", Instant.now().plus(1, ChronoUnit.HOURS));
    }

    private static String build(String secret, Instant expiration) {
        try {
            var claims = new JWTClaimsSet.Builder()
                    .subject("ec4462ba-f6f5-4fa3-9458-207f5ec9bd17")
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(expiration))
                    .build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("не удалось подписать тестовый JWT", e);
        }
    }
}
