package com.dating.notification.mvp.reps;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProcessedEvents {
    private final Set<UUID> eventBase = ConcurrentHashMap.newKeySet();

    public boolean markIfNew(UUID eventId) {
        return eventBase.add(eventId);
    }
}
