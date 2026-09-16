package com.dating.notification.kafka.listeners;

import com.dating.notification.kafka.events.UserRegistered;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationListener {

    private static final Logger log = LogManager.getLogger(RegistrationListener.class);

    // TODO(bug): нет дедупликации по eventId (в отличие от MatchListener), хотя README и CODEBASE
    //  заявляют идемпотентность обоих топиков. At-least-once даст повторную обработку.
    // TODO(security): email в логе — PII. Потребителю email не нужен, достаточно userId;
    //  убрать поле из события UserRegistered и задать топикам ретенцию.
    // TODO(debt): здесь и в PseudoRabbitMQNotificationService используется log4j2 API напрямую,
    //  а в MatchListener — SLF4J. Привести к одному (SLF4J).
    @KafkaListener(topics = "user-events")
    public void listen(UserRegistered event) {
        log.info("User {} with id = {} registered", event.email(), event.userId());
    }
}
