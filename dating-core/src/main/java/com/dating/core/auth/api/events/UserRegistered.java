package com.dating.core.auth.api.events;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("user-events::#{userId()}")
public record UserRegistered(UUID userId, String email, String displayName, UUID eventId, Instant occurredAt) {
}
