package com.dating.core;

import com.dating.core.auth.api.events.UserRegistered;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Тестовый консюмер: собирает события из топика {@code user-events}.
 *
 * <p>ВАЖНО: JSON здесь десериализуется в {@link UserRegistered} без явной
 * конфигурации только потому, что spring-modulith-events-kafka приносит в
 * контекст конвертер сообщений. Отдельный сервис-консюмер без этой зависимости
 * так работать не будет — ему нужен свой JsonDeserializer/конвертер
 * (поищи: spring-kafka JsonDeserializer trusted packages).
 */
@TestConfiguration
public class TestKafkaConsumer {
    final List<UserRegistered> received = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "user-events", groupId = "test-consumer")
    void collect(UserRegistered event) {
        received.add(event);
    }
}
