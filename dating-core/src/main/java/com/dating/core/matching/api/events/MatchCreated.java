package com.dating.core.matching.api.events;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("user-matching-events::#{matchId()}")
public record MatchCreated(UUID matchId, UUID userLow, UUID userHigh, UUID eventId, Instant createdAt) {
}
