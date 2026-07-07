package com.dating.notification.kafka.events;

import java.time.Instant;
import java.util.UUID;

public record UserRegistered(UUID userId, String email, String displayName, UUID eventId, Instant occurredAt) {
}
