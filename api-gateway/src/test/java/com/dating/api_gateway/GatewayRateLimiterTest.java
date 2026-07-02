package com.dating.api_gateway;

import com.dating.api_gateway.support.GatewayIntegrationTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rate-limiter (Redis token-bucket) режет всплеск запросов сверх лимита статусом 429.
 *
 * Конфиг (application.yaml): replenishRate=10, burstCapacity=20. Ключ — IP клиента
 * (userKeyResolver -> getRemoteAddress). Значит все тесты бьют с одного localhost-IP и
 * делят одно ведро в Redis. Если выбить > burstCapacity запросов быстрее, чем ведро
 * пополняется — часть ответов станет 429.
 *
 * ПОЧЕМУ свой X-Forwarded-For: отдельный класс сам по себе НЕ изолирует — у всех
 * наследников GatewayIntegrationTest одинаковая конфигурация, поэтому Spring кеширует
 * и переиспользует ОДИН контекст (и один Redis) на все тест-классы. Без изоляции этот
 * тест исчерпал бы ведро localhost, и тесты, бегущие следом, ловили бы 429 (флак).
 * Поэтому шлём запросы с X-Forwarded-For: KeyResolver (maxTrustedIndex=1) возьмёт IP
 * из заголовка, и у этого теста будет СВОЁ ведро, не пересекающееся с остальными.
 * Не полагаемся на конкретный порядковый номер ответа — проверяем лишь факт:
 * среди залпа появился хотя бы один 429.
 *
 * ОГОВОРКА для собеса: тест доказывает, что фильтр включён и режет всплеск, но не точную
 * границу (пополнение ведра во времени и сетевые задержки делают точный счётчик хрупким).
 * Для точной проверки алгоритма лучше юнит-тест над RedisRateLimiter, а не E2E.
 */
class GatewayRateLimiterTest extends GatewayIntegrationTest {

    /** IP из диапазона TEST-NET-3 (RFC 5737) — выделен специально для примеров и тестов. */
    private static final String FAKE_CLIENT_IP = "203.0.113.7";

    @Test
    void burstOverLimit_getsThrottled() {
        // upstream всегда отвечает 201 — чтобы всё, что не зарезал limiter, было именно 201.
        stubFor(post(urlEqualTo("/api/auth/register"))
                .willReturn(aResponse().withStatus(201)));

        int requests = 40;   // заведомо больше burstCapacity=20
        boolean sawThrottling = false;

        for (int i = 0; i < requests; i++) {
            int status = webClient.post().uri("/api/auth/register")
                    .header("X-Forwarded-For", FAKE_CLIENT_IP)   // своё ведро, см. javadoc класса
                    .bodyValue("{\"email\":\"a@b.com\",\"password\":\"password123\",\"displayName\":\"Ann\"}")
                    .exchange()
                    .returnResult(Void.class)
                    .getStatus()
                    .value();
            if (status == 429) {   // TOO_MANY_REQUESTS
                sawThrottling = true;
                break;
            }
        }

        assertThat(sawThrottling)
                .as("залп из %d запросов сверх burstCapacity=20 должен словить хотя бы один 429", requests)
                .isTrue();
    }
}
