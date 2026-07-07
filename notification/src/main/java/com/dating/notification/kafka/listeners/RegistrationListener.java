package com.dating.notification.kafka.listeners;

import com.dating.notification.kafka.events.UserRegistered;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationListener {

    private static final Logger log = LogManager.getLogger(RegistrationListener.class);

    @KafkaListener(topics = "user-events")
    public void listen(UserRegistered event) {
        log.info("User {} with id = {} registered", event.email(), event.userId());
    }
}
