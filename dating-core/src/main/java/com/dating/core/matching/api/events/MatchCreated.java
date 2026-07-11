package com.dating.core.matching.api.events;

import com.dating.core.matching.domain.Match;
import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("user-matching-events::#{matchId()}")
public record MatchCreated(UUID matchId, UUID userLow, UUID userHigh, UUID eventId, Instant createdAt) {
    public static MatchCreated from(Match match) {
        return new MatchCreated(
                match.getId(),
                match.getUserLow(),
                match.getUserHigh(),
                UUID.randomUUID(),
                Instant.now()
        );
    }
}
