package com.dating.api_gateway;

import com.dating.api_gateway.support.GatewayIntegrationTest;
import com.dating.api_gateway.support.JwtTestTokens;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Security-контур гейтвея: кого пускать на upstream, кого резать на входе.
 *
 * Ключевая идея всех негативных тестов: если гейтвей отбил запрос сам (401), он НЕ должен
 * был сходить на upstream. Это проверяем через verify(..., exactly(0)) — WireMock не получил
 * ни одного запроса. Так тест доказывает, что защита стоит НА гейтвее, а не «протекает» на core.
 */
class GatewaySecurityTest extends GatewayIntegrationTest {

    /** Защищённый путь без токена => 401, до upstream не доходит. */
    @Test
    void protectedPath_withoutToken_isUnauthorized() {
        stubFor(get(urlEqualTo("/api/profiles/me"))
                .willReturn(aResponse().withStatus(200)));

        webClient.get().uri("/api/profiles/me")
                .exchange()
                .expectStatus().isUnauthorized();   // 401 от самого гейтвея

        // upstream не должен был получить запрос — гейтвей отбил его раньше.
        verify(exactly(0), getRequestedFor(urlEqualTo("/api/profiles/me")));
    }

    /** Токен с чужой подписью => 401 (гейтвей проверяет подпись своим секретом). */
    @Test
    void protectedPath_withBadSignature_isUnauthorized() {
        stubFor(get(urlEqualTo("/api/profiles/me"))
                .willReturn(aResponse().withStatus(200)));

        webClient.get().uri("/api/profiles/me")
                .header("Authorization", "Bearer " + JwtTestTokens.wrongSignature())
                .exchange()
                .expectStatus().isUnauthorized();

        verify(exactly(0), getRequestedFor(urlEqualTo("/api/profiles/me")));
    }

    /** Просроченный токен (подпись верна, exp в прошлом) => 401. */
    @Test
    void protectedPath_withExpiredToken_isUnauthorized() {
        stubFor(get(urlEqualTo("/api/profiles/me"))
                .willReturn(aResponse().withStatus(200)));

        webClient.get().uri("/api/profiles/me")
                .header("Authorization", "Bearer " + JwtTestTokens.expiredHs256(jwtSecret))
                .exchange()
                .expectStatus().isUnauthorized();

        verify(exactly(0), getRequestedFor(urlEqualTo("/api/profiles/me")));
    }

    /** Публичный путь /api/auth/** доступен без токена (permitAll) — контроль на контраст. */
    @Test
    void publicPath_withoutToken_isAllowedThrough() {
        stubFor(post(urlEqualTo("/api/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accessToken\":\"x\"}")));

        webClient.post().uri("/api/auth/login")
                .bodyValue("{\"email\":\"a@b.com\",\"password\":\"password123\"}")
                .exchange()
                .expectStatus().isOk();

        // здесь наоборот — запрос ДОЛЖЕН был дойти до upstream.
        verify(postRequestedFor(urlEqualTo("/api/auth/login")));
    }
}
