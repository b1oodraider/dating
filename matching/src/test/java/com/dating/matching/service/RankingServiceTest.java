package com.dating.matching.service;

import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.matching.dto.Criteria;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class RankingServiceTest {
    RankingService rs = new RankingService();

    @Test
    public void score_genderMismatch_returnsZero() {
        int N_years_age = 24;
        assertThat(rs.score(candidate("male", "Moscow", LocalDate.now().minusYears(N_years_age).toString()),
                new Criteria(24, "Moscow", "female"))).isZero();
    }

    @Test
    public void score_sameCity_addsBonus() {
        var me = new Criteria(24, "Moscow", "female");
        int N_years_age = 24;
        assertThat(rs.score(candidate("female", "Moscow", LocalDate.now().minusYears(N_years_age).toString()), me))
                .isGreaterThan(rs.score(candidate("female", "SPB", LocalDate.now().minusYears(N_years_age).toString()), me));
    }

    @Test
    public void score_closerAge_scoresHigher() {
        int firstCandidateAge = 24;
        int secondCandidateAge = 19;
        Criteria me = new Criteria(27, "Moscow", "female");
        assertThat(rs.score(candidate("female", "Moscow", LocalDate.now().minusYears(firstCandidateAge).toString()), me))
                .isGreaterThan(rs.score(candidate("female", "Moscow", LocalDate.now().minusYears(secondCandidateAge).toString()), me));
    }

    @Test
    public void score_emptyBirthDate_skipsAgeComponent() {
        Criteria me = new Criteria(27, "Moscow", "female");
        assertThatNoException().isThrownBy(() -> rs.score(candidate("female", "Moscow", ""), me));
    }

    private ProfileMessage candidate(String gender, String city, String birthDate) {
        return ProfileMessage.newBuilder().setGender(gender).setCity(city).setBirthDate(birthDate).build();
    }
}
