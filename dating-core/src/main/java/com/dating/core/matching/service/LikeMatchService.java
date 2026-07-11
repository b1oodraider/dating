package com.dating.core.matching.service;

import com.dating.core.matching.api.events.MatchCreated;
import com.dating.core.matching.dto.NewLike;
import com.dating.core.matching.domain.Like;
import com.dating.core.matching.domain.Match;
import com.dating.core.matching.repo.LikeRepository;
import com.dating.core.matching.repo.MatchRepository;
import jakarta.transaction.Transaction;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class LikeMatchService {
    private final LikeRepository likeRepository;

    private final MatchRepository matchRepository;

    private final ApplicationEventPublisher events;

    private final TransactionTemplate tt;

    public LikeMatchService(LikeRepository likeRepository, MatchRepository matchRepository, ApplicationEventPublisher events, TransactionTemplate tt) {
        this.likeRepository = likeRepository;
        this.matchRepository = matchRepository;
        this.events = events;
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.tt = tt;
    }

    @Transactional
    public boolean setLike(UUID fromUserId, UUID toUserId) {
        try {
            var like = tt.execute(_ -> likeRepository.saveAndFlush(new Like(fromUserId, toUserId)));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    // TODO(edge): окно сбоя — процесс упал МЕЖДУ коммитом setLike и setMatch → взаимный лайк
    //  без матча навсегда (следующего лайка не будет — unique). Починка: сверка при чтении
    //  или фоновая ре-проверка взаимных лайков без матча.
    @Transactional
    public boolean setMatch(UUID fromUserId, UUID toUserId) {
        // проверяем на наличие обратного лайка
        if (!likeRepository.existsByFromUserIdAndToUserId(toUserId, fromUserId)) {return false;}

        UUID low, high;

        low = (toUserId.compareTo(fromUserId) < 0) ? toUserId : fromUserId;
        high = (fromUserId.compareTo(toUserId) > 0) ? fromUserId : toUserId;

        var match = new Match(low, high);

        try {
            tt.execute(_ -> {
                var saved = matchRepository.saveAndFlush(match);
                events.publishEvent(new MatchCreated(saved.getId(), low, high, UUID.randomUUID(), Instant.now()));

            return saved;
            });
            return true;

        } catch (DataIntegrityViolationException e) {
            return matchRepository.existsByUserLowAndUserHigh(match.getUserLow(), match.getUserHigh());
        }
    }
}
