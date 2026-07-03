package com.dating.matching.service;

import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.matching.dto.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Service
public class RankingService {
    // TODO: заменить на обращение к сервису аналитики, когда тот будет готов
    private static final int ENGAGEMENT_SCORE = 1;

    public double score(ProfileMessage candidate, Criteria me){
        double score = 0;
        if(!candidate.getGender().equals(me.gender())) {
            return score;
        }

        score += (!candidate.getCity().isBlank() && candidate.getCity().equals(me.city()))? 10 : 0;

        if(!candidate.getBirthDate().isEmpty() && !candidate.getBirthDate().isBlank()) {
            LocalDate birthDate = LocalDate.parse(candidate.getBirthDate());
            int candidateAge = Period.between(birthDate, LocalDate.now()).getYears();
            score -= Math.abs(candidateAge - me.age());
        }

        score += ENGAGEMENT_SCORE;

        return score;
    }
}
