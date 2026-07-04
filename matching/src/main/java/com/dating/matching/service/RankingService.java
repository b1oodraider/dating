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

    // TODO(баг): несовпадение пола даёт score=0, а подходящий кандидат может уйти в минус
    //  (штраф за возраст > бонусов), т.е. неподходящий по полу отранжируется ВЫШЕ подходящего,
    //  и recommend() его не отфильтрует. Пол — жёсткий фильтр: кандидатов не того пола нужно
    //  исключать из выдачи целиком (например, Optional.empty()/фильтр до ранжирования),
    //  а не возвращать 0.
    public double score(ProfileMessage candidate, Criteria me){
        double score = 0;
        if(!candidate.getGender().equals(me.gender())) {
            return score;
        }

        score += (!candidate.getCity().isBlank() && candidate.getCity().equals(me.city()))? 10 : 0;

        // TODO(баг): LocalDate.parse на кривой строке кинет DateTimeParseException, и весь
        //  /recommendations ответит 500 из-за одного кандидата. Обернуть в try/catch, как уже
        //  сделано в RecommendationService.messageToDTO (и isEmpty() лишний — isBlank() его включает).
        if(!candidate.getBirthDate().isBlank()) {
            LocalDate birthDate = LocalDate.parse(candidate.getBirthDate());
            int candidateAge = Period.between(birthDate, LocalDate.now()).getYears();
            score -= Math.abs(candidateAge - me.age());
        }

        score += ENGAGEMENT_SCORE;

        return score;
    }
}
