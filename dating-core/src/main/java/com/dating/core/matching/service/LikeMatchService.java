package com.dating.core.matching.service;

import com.dating.core.matching.api.events.MatchCreated;
import com.dating.core.matching.dto.NewLike;
import com.dating.core.matching.domain.Like;
import com.dating.core.matching.domain.Match;
import com.dating.core.matching.repo.LikeRepository;
import com.dating.core.matching.repo.MatchRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LikeMatchService {
    private final LikeRepository likeRepository;

    private final MatchRepository matchRepository;

    private final ApplicationEventPublisher events;

    public LikeMatchService(LikeRepository likeRepository, MatchRepository matchRepository, ApplicationEventPublisher events) {
        this.likeRepository = likeRepository;
        this.matchRepository = matchRepository;
        this.events = events;
    }

    @Transactional
    public boolean setLike(UUID fromUserId, UUID toUserId) {
        try {
            likeRepository.saveAndFlush(new Like(fromUserId, toUserId));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    // Дизайн (важно понимать, почему это работает): setLike и setMatch — ДВЕ отдельные транзакции.
    // Проверка взаимности идёт после коммита собственного лайка, поэтому чей лайк закоммитился
    // вторым — тот гарантированно видит первый → «потерянный матч» невозможен; дубль ловит
    // unique(user_low, user_high). Оба исхода гонки закрыты (см. Guide-Day8).
    // TODO(edge): окно сбоя — процесс упал МЕЖДУ коммитом setLike и setMatch → взаимный лайк
    //  без матча навсегда (следующего лайка не будет — unique). Починка: сверка при чтении
    //  или фоновая ре-проверка взаимных лайков без матча.
    @Transactional
    public boolean setMatch(UUID fromUserId, UUID toUserId) {
        // проверяем на наличие обратного лайка
        if (!likeRepository.existsByFromUserIdAndToUserId(toUserId, fromUserId)) {return false;}

        try {
            UUID low, high;

            low = (toUserId.compareTo(fromUserId) < 0) ? toUserId : fromUserId;
            high = (fromUserId.compareTo(toUserId) > 0) ? fromUserId : toUserId;

            var match = new Match(low, high);
            Match matchEntity = matchRepository.saveAndFlush(match);

            events.publishEvent(new MatchCreated(matchEntity.getId(), low, high, UUID.randomUUID(), Instant.now()));

            return true;

        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
