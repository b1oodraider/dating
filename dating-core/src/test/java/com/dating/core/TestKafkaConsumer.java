package com.dating.core;

import com.dating.core.auth.api.events.UserRegistered;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
public class TestKafkaConsumer {
    final List<UserRegistered> received = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "user-events", groupId = "test-consumer")
    void collect(UserRegistered event) {
        received.add(event);
    }
}
