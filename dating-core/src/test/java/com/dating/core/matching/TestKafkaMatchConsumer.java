package com.dating.core.matching;


import com.dating.core.matching.api.events.MatchCreated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
public class TestKafkaMatchConsumer {
    final List<MatchCreated> received = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "user-matching-events", groupId = "test-consumer")
    void collect(MatchCreated event) {
        received.add(event);
    }
}
