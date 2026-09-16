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

    // TODO(bug): markIfNew вызывается ДО доставки — как только notifyMatch станет реальной отправкой,
    //  её падение оставит событие помеченным, Kafka-ретрай уйдёт в ветку "дубль", уведомление
    //  потеряется молча. Это не at-least-once + идемпотентность, а at-most-once. Порядок
    //  "доставить -> пометить" (или общая транзакция) решить ДО перехода на RabbitMQ.
    // TODO(bug): аргументы переставлены — notifyMatch(UUID userLow, UUID userHigh) вызывается как
    //  notifyMatch(event.userHigh(), event.userLow()). Сейчас это только текст лога.
    @KafkaListener(topics = "user-matching-events")
    public void listen(MatchCreated event) {
        if(!events.markIfNew(event.eventId())) {
            log.info("Дубль eventId={}, пропуск", event.eventId());
            return;
        }
            ns.notifyMatch(event.userHigh(), event.userLow());

    }
}
