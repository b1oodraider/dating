package com.dating.core.matching;


import com.dating.core.matching.api.events.MatchCreated;
import com.dating.core.matching.domain.Match;
import com.dating.core.matching.repo.LikeRepository;
import com.dating.core.matching.repo.MatchRepository;
import com.dating.core.matching.service.LikeMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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

    @InjectMocks
    LikeMatchService serv;

    @Mock
    ApplicationEventPublisher publisher;


    @Test
    public void sortOnCreation() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID low = (first.compareTo(second) < 0) ? first : second;
        UUID high = (first.compareTo(second) < 0) ? second : first;

        Match m1 = new Match(high, low);
        Match m2 = new Match(low, high);

        assertThat(m1.getUser_low()).isEqualTo(m2.getUser_low());
        assertThat(m1.getUser_high()).isEqualTo(m2.getUser_high());
        assertThat(m1.getUser_low().compareTo(m2.getUser_high())).isLessThan(0);
    }

    @Test
    public void like_match_flow() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        when(likeRepository.existsByFromUserIdAndToUserId(user2, user1)).thenReturn(false);
        assertThat(serv.setMatch(user1, user2)).isFalse();
        verify(matchRepository, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);

        when(likeRepository.existsByFromUserIdAndToUserId(user1, user2)).thenReturn(true);
        when(matchRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(serv.setMatch(user2, user1)).isTrue();
        verify(matchRepository).saveAndFlush(any());
        verify(publisher).publishEvent(any(MatchCreated.class));
    }
}
