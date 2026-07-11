package com.dating.core.matching.repo;

import com.dating.core.matching.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    boolean existsByUserLowAndUserHigh(UUID userLow, UUID userHigh);
}
