package com.dating.notification.kafka.events;

import java.time.Instant;
import java.util.UUID;

public record MatchCreated(UUID matchId, UUID userLow, UUID userHigh, UUID eventId, Instant createdAt) {
}
