package com.dating.notification.mvp.reps;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
// TODO(ops): дедуп в неограниченном in-memory Set — растёт бесконечно (утечка), теряется при
//  рестарте и не работает при нескольких инстансах. Durable-вариант: processed_events(event_id PK),
//  insert -> on conflict пропуск, в идеале в одной транзакции с доставкой.
public class ProcessedEvents {
    private final Set<UUID> eventBase = ConcurrentHashMap.newKeySet();

    public boolean markIfNew(UUID eventId) {
        return eventBase.add(eventId);
    }
}
