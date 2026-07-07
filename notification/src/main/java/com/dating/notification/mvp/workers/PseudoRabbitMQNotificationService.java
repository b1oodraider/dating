package com.dating.notification.mvp.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PseudoRabbitMQNotificationService {

    private static final Logger log = LogManager.getLogger(PseudoRabbitMQNotificationService.class);

    public void notifyMatch(UUID userLow, UUID userHigh) {
        log.info("Уведомление: у пользователей {} и {} мэтч", userLow.toString(), userHigh.toString());
    }
}
