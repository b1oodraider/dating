package com.dating.api_gateway;

import com.dating.api_gateway.support.GatewayIntegrationTest;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * E2E happy-path: gateway корректно проксирует /api/** на upstream (роль core играет WireMock).
 *
 * Что доказываем: запрос проходит security + rate-limiter, форвардится на WireMock,
 * ответ WireMock (статус, тело) возвращается клиенту без искажений. Настоящий core не поднимаем.
 *
 * Поток:  WebTestClient -> api-gateway (route /api/**) -> WireMock (fake core)
 *
 * Инфраструктура (подъём контекста, WireMock, Redis, JWT-хелпер) — в GatewayIntegrationTest.
 */
@ActiveProfiles("test")
class E2EProxyTest extends GatewayIntegrationTest {

    /**
     * Публичный путь /api/auth/** — единственный permitAll, JWT не нужен.
     * Самый чистый тест проксирования: без auth-обвязки.
     */
    @Test
    void register_isProxiedToCore() {
        // stub: WireMock (в роли core) отвечает 201 на POST /api/auth/register.
        //   201 CREATED + пустое тело — реальный контракт AuthController.
        //   До стаба любой запрос вернул бы 404.
        stubFor(post(urlEqualTo("/api/auth/register"))
                .willReturn(aResponse().withStatus(201)));

        // act: POST в gateway с телом (три поля RegisterRequest). Для WireMock тело неважно
        //   (стаб по телу не матчит), но так тест ближе к реальности.
        webClient.post().uri("/api/auth/register")
                .bodyValue("""
                        {"email":"a@b.com","password":"password123","displayName":"Ann"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        // verify: подтверждаем, что gateway РЕАЛЬНО сходил на upstream, а не ответил сам.
        verify(postRequestedFor(urlEqualTo("/api/auth/register")));
    }

    /**
     * Защищённый путь /api/profiles/** требует валидный JWT (не в permitAll).
     * Проверяем и проксирование, и что тело от core доходит до клиента целым.
     */
    @Test
    void profileMe_withValidJwt_isProxiedToCore() {
        // stub: core отдаёт 200 + JSON. Путь во МНОЖЕСТВЕННОМ числе (ProfileController:
        //   @RequestMapping("/api/profiles") + @GetMapping("/me")). Content-Type обязателен.
        stubFor(get(urlEqualTo("/api/profiles/me"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"ec4462ba","displayName":"Ann"}
                                """)));

        webClient.get().uri("/api/profiles/me")
                .header("Authorization", bearer())   // валидный токен из базового класса
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.displayName").isEqualTo("Ann");   // тело core дошло до клиента

        verify(getRequestedFor(urlEqualTo("/api/profiles/me")));
    }

    @Test
    void upstreamError_returnsFallback() {
        stubFor(get(urlEqualTo("/api/profiles/me"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        webClient.get().uri("/api/profiles/me")
                .header("Authorization", bearer())
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.error").isEqualTo("Service unavailable");
        verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/api/profiles/me")));
    }

    @Test
    void upstreamTimeout_returnsFallback() {
        stubFor(get(urlEqualTo("/api/profiles/me"))
                .willReturn(aResponse().withFixedDelay(12000)));

        webClient.get().uri("/api/profiles/me")
                .header("Authorization", bearer())
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.error").isEqualTo("Service unavailable");
        verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/api/profiles/me")));
    }
}
