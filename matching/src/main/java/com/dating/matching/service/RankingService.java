package com.dating.matching.service;

import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.matching.dto.Criteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

@Service
public class RankingService {
    // TODO: заменить на обращение к сервису аналитики, когда тот будет готов
    private static final int ENGAGEMENT_SCORE = 1;
    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    // TODO(баг): несовпадение пола даёт score=0, а подходящий кандидат может уйти в минус
    //  (штраф за возраст > бонусов), т.е. неподходящий по полу отранжируется ВЫШЕ подходящего,
    //  и recommend() его не отфильтрует. Пол — жёсткий фильтр: кандидатов не того пола нужно
    //  исключать из выдачи целиком (например, Optional.empty()/фильтр до ранжирования),
    //  а не возвращать 0.
    //  UPDATE: баг пофиксил, все что имеет score=0 отфильтруется на стримах и не попадет в выборку,
    //  осталось убрать пол и возрастные рамки в транзакции
    // TODO(bug): возрастной штраф сравнивается с городским бонусом (score = ageDiff > score ? 0 : ...).
    //  Итог: все иногородние получают ровно 1 независимо от возраста — возрастное ранжирование для
    //  них не работает вовсе. Считать штраф отдельным слагаемым со своим весом.
    // TODO(bug): proto3 presence — незаполненный gender приходит как "" и трактуется как "не тот пол":
    //  только что зарегистрированный пользователь (ProfileCreatorImpl создаёт пустой профиль) молча
    //  невидим в выдаче. Плюс сравнение регистрозависимое, gender ничем не нормализован.
    // TODO(bug): пустой birthDate = нулевой штраф => незаполненная анкета ранжируется как идеально
    //  совпавшая по возрасту и систематически всплывает наверх.
    // TODO(bug): "0 = не подходит" нигде не выражено явно — отсев держится на том, что
    //  ENGAGEMENT_SCORE=1. Изменят константу на 0 — фильтр score>0 начнёт резать подходящих.
    public double score(ProfileMessage candidate, Criteria me){
        double score = 0;
        if(!candidate.getGender().equals(me.gender())) {
            return score;
        }

        score += (!candidate.getCity().isBlank() && candidate.getCity().equals(me.city()))? 10 : 0;

        try {
            if (!candidate.getBirthDate().isBlank()) {
                LocalDate birthDate = LocalDate.parse(candidate.getBirthDate());
                int candidateAge = Period.between(birthDate, LocalDate.now()).getYears();
                int ageDiff = Math.abs(candidateAge - me.age());
                //TODO: скорректировать отбор по возрасту – закинуть его в транзакцию вместе с полом
                score = (ageDiff > score)? 0 : score-ageDiff;
            }
        } catch (DateTimeParseException e){
            log.error("Wrong DateTime format of birthDate in profile with id={} and userId={}", candidate.getId(), candidate.getUserId());
        }
        score += ENGAGEMENT_SCORE;

        return score;
    }
}
