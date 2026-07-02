package com.dating.api_gateway.support;

import com.dating.api_gateway.TestcontainersConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Общий базовый класс для всех интеграционных тестов api-gateway.
 *
 * Зачем базовый класс: аннотации подъёма контекста (@SpringBootTest, @WireMockTest,
 * @Import) и общие бины (WebTestClient, jwtSecret) одинаковы во всех тестах модуля.
 * Выносим их сюда, чтобы не дублировать и чтобы контекст переиспользовался Spring-ом
 * между тест-классами (одинаковая конфигурация => один закешированный ApplicationContext
 * => тесты быстрее).
 *
 * Аннотации:
 *  @SpringBootTest(RANDOM_PORT) — полный контекст гейтвея на случайном реальном порту.
 *      RANDOM_PORT обязателен: WebTestClient должен к чему-то подключаться (при MOCK
 *      реального порта нет). Клиент автоконфигурируется на этот порт.
 *  @WireMockTest(httpPort = 8081) — WireMock на localhost:8081, поднимается до теста,
 *      глушится после. 8081 совпадает с дефолтом CORE_URI в application.yaml, поэтому
 *      route /api/** гейтвея без доп. настройки бьёт ровно в WireMock.
 *  @Import(TestcontainersConfiguration.class) — Redis-контейнер (нужен гейтвею на старте:
 *      rate-limiter + reactive-redis). Требует запущенного Docker.
 *  @AutoConfigureWebTestClient — в Boot 4 бин WebTestClient сам не появляется
 *      (нужны зависимость spring-boot-starter-webflux-test И эта аннотация);
 *      при RANDOM_PORT клиент привязывается к реально поднятому серверу.
 *
 * abstract — сам по себе не запускается, только наследуется конкретными тест-классами.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WireMockTest(httpPort = 8081)
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
public abstract class GatewayIntegrationTest {

    /** Клиент, которым шлём запросы В gateway (не напрямую в WireMock). */
    @Autowired
    protected WebTestClient webClient;

    /**
     * Тот же секрет, которым гейтвей ВАЛИДИРУЕТ подпись JWT (SecurityConfig, app.jwt.secret).
     * Нужен, чтобы ПОДПИСАТЬ тестовый токен тем же ключом — иначе гейтвей вернёт 401.
     */
    @Value("${app.jwt.secret}")
    protected String jwtSecret;

    /** Валидный Bearer-заголовок с токеном, подписанным секретом гейтвея. */
    protected String bearer() {
        return "Bearer " + JwtTestTokens.validHs256(jwtSecret);
    }
}
