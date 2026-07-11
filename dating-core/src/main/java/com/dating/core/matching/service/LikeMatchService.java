package com.dating.core.matching.service;

import com.dating.core.matching.api.events.MatchCreated;
import com.dating.core.matching.domain.Like;
import com.dating.core.matching.domain.Match;
import com.dating.core.matching.repo.LikeRepository;
import com.dating.core.matching.repo.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
public class LikeMatchService {
    private static final Logger log = LoggerFactory.getLogger(LikeMatchService.class);
    private final LikeRepository likeRepository;

    private final MatchRepository matchRepository;

    private final ApplicationEventPublisher events;

    private final PlatformTransactionManager txManager;

    public LikeMatchService(LikeRepository likeRepository, MatchRepository matchRepository, ApplicationEventPublisher events, PlatformTransactionManager txManager) {
        this.likeRepository = likeRepository;
        this.matchRepository = matchRepository;
        this.events = events;
        this.txManager = txManager;
    }

    public boolean setLike(UUID fromUserId, UUID toUserId) {
        try {
            requiresNew(_ -> likeRepository.saveAndFlush(new Like(fromUserId, toUserId)));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Check if it's not Duplicate but still exception", e);
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

        var match = new Match(fromUserId, toUserId);

        try {
            requiresNew(_ -> {
                var saved = matchRepository.saveAndFlush(match);
                events.publishEvent(MatchCreated.from(saved));

            return saved;
            });
            return true;

        } catch (DataIntegrityViolationException e) {
            log.debug("Check if it's not Duplicate but still exception", e);
            return matchRepository.existsByUserLowAndUserHigh(match.getUserLow(), match.getUserHigh());
        }
    }

    private <T> T requiresNew(TransactionCallback<T> callback) {

        var tt = new TransactionTemplate(txManager);

        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return tt.execute(callback);
    }
}
