package com.dating.notification.kafka.listeners;

import com.dating.notification.kafka.events.MatchCreated;
import com.dating.notification.mvp.reps.ProcessedEvents;
import com.dating.notification.mvp.workers.PseudoRabbitMQNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MatchListener {
    private static final Logger log = LoggerFactory.getLogger(MatchListener.class);
    final PseudoRabbitMQNotificationService ns;
    final ProcessedEvents events;

    public MatchListener(PseudoRabbitMQNotificationService ns, ProcessedEvents events) {
        this.ns = ns;
        this.events = events;
    }

    @KafkaListener(topics = "user-matching-events")
    public void listen(MatchCreated event) {
        if(!events.markIfNew(event.eventId())) {
            log.info("Дубль eventId={}, пропуск", event.eventId());
            return;
        }
            ns.notifyMatch(event.userHigh(), event.userLow());

    }
}
