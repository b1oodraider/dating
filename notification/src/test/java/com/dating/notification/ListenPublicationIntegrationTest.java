package com.dating.notification;

import com.dating.notification.mvp.reps.ProcessedEvents;
import com.dating.notification.mvp.workers.PseudoRabbitMQNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
public class ListenPublicationIntegrationTest {

    @MockitoSpyBean
    PseudoRabbitMQNotificationService ns;

    @MockitoSpyBean
    ProcessedEvents events;

    @Autowired
    private KafkaTemplate<String, String> template;

    private static final String TOPIC = "user-matching-events";

    @BeforeEach
    void reset() { clearInvocations(ns, events); }


    @Test
    public void oneEventListenTest() {
        template.send(TOPIC, matchJson(UUID.randomUUID()));
        await().atMost(15, SECONDS).untilAsserted(()->verify(ns, times(1)).notifyMatch(any(), any()));
    }

    @Test
    public void dublesCheckTest() {
        UUID e = UUID.randomUUID();
        template.send(TOPIC, matchJson(e));
        template.send(TOPIC, matchJson(e));
        await().atMost(30, SECONDS).untilAsserted(() ->
                verify(events, times(2)).markIfNew(any()));
        verify(ns, times(1)).notifyMatch(any(), any());
    }

    @Test
    public void manyMatchesCheckTest() {
        template.send(TOPIC, matchJson(UUID.randomUUID()));
        template.send(TOPIC, matchJson(UUID.randomUUID()));
        await().atMost(30, SECONDS).untilAsserted(() -> verify(ns, times(2)).notifyMatch(any(), any()));
    }

    private String matchJson(UUID eventId) {
        return """
        {"matchId":"%s","userLow":"%s","userHigh":"%s","eventId":"%s","createdAt":"2026-07-07T21:34:58.892540113Z"}
        """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), eventId).strip();
    }
}
