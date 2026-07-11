package com.dating.core.matching;


import com.dating.core.matching.api.events.MatchCreated;
import com.dating.core.matching.domain.Match;
import com.dating.core.matching.repo.LikeRepository;
import com.dating.core.matching.repo.MatchRepository;
import com.dating.core.matching.service.LikeMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchTest {

    @Mock
    LikeRepository likeRepository;

    @Mock
    MatchRepository matchRepository;

    @Mock
    TransactionTemplate tt;

    @InjectMocks
    LikeMatchService service;

    @Mock
    ApplicationEventPublisher publisher;

    @BeforeEach
    void stubTransactionTemplateToRunCallback() {
        lenient().when(tt.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
    }

    @Test
    public void sortOnCreation() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID low = (first.compareTo(second) < 0) ? first : second;
        UUID high = (first.compareTo(second) < 0) ? second : first;

        Match m1 = new Match(high, low);
        Match m2 = new Match(low, high);

        assertThat(m1.getUserLow()).isEqualTo(m2.getUserLow());
        assertThat(m1.getUserHigh()).isEqualTo(m2.getUserHigh());
        assertThat(m1.getUserLow().compareTo(m2.getUserHigh())).isLessThan(0);
    }

    @Test
    public void like_match_flow() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        when(likeRepository.existsByFromUserIdAndToUserId(user2, user1)).thenReturn(false);
        assertThat(service.setMatch(user1, user2)).isFalse();
        verify(matchRepository, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);

        when(likeRepository.existsByFromUserIdAndToUserId(user1, user2)).thenReturn(true);
        when(matchRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.setMatch(user2, user1)).isTrue();
        verify(matchRepository).saveAndFlush(any());
        verify(publisher).publishEvent(any(MatchCreated.class));
    }

    @Test
    void setMatch_uniqueViolationOnInsert_butMatchAlreadyExists_returnsTrue() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID low  = a.compareTo(b) < 0 ? a : b;
        UUID high = a.compareTo(b) < 0 ? b : a;

        when(likeRepository.existsByFromUserIdAndToUserId(b, a)).thenReturn(true);

        when(matchRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_matches violation"));
        when(matchRepository.existsByUserLowAndUserHigh(low, high)).thenReturn(true);

        boolean result = service.setMatch(a, b);

        assertThat(result).isTrue();
        verify(matchRepository).existsByUserLowAndUserHigh(low, high);
        verify(publisher, never()).publishEvent(any());
    }
}
