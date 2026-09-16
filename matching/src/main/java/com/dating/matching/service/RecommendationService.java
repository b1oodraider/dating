package com.dating.matching.service;

import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.matching.dto.RecommendationDTO;
import com.dating.matching.dto.Criteria;
import com.dating.matching.service.records.Scored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private final CandidateProfileFetcher fetcher;
    private final RankingService rankings;

    public RecommendationService(CandidateProfileFetcher fetcher, RankingService rankings) {
        this.fetcher = fetcher;
        this.rankings = rankings;
    }

    // TODO(arch): hydration идёт N унарными gRPC (fetchCandidateProfiles), а готовый батч
    //  batchFetchCandidateProfiles + MAX_BATCH_SIZE=100 в core не вызывается ниоткуда (мёртвый код).
    //  topK=100 => 101 round-trip и 100 конкурентных запросов в пул Hikari на 10 соединений.
    // TODO(arch): при недоступном core выдача — 200 с пустым списком (все StatusRuntimeException
    //  проглочены). Клиент не отличит "кандидатов нет" от "core лёг". Выбрать и зафиксировать
    //  поведение: best-effort уместен при отказе ЧАСТИ фан-аута, а не всех.
    public List<RecommendationDTO> recommend(UUID userId, int topK) {
        List<UUID> recommendedProfiles = selectCandidateIds(userId);
        List<ProfileMessage> profiles = fetcher.fetchCandidateProfiles(recommendedProfiles);
        // TODO: критерии из профиля запросившего (GetProfileByUserId gRPC) — сейчас заглушка
        Criteria me = new Criteria(28, "Moscow", "female");
        return profiles.stream()
                .map(p-> new Scored(p, rankings.score(p, me)))
                .filter(p-> p.score() > 0)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(topK)
                .map(Scored::profile)
                .map(this::messageToDTO)
                .toList();
    }

    // TODO: реальная выборка кандидатов (Neo4j/фильтры) — вне первого спринта
    // TODO(arch): планируемый FindCandidates(user_id, limit) без ORDER BY вернёт произвольный срез —
    //  ranking будет ранжировать случайную выборку, а пользователь получать один и тот же набор.
    //  Нужен детерминированный порядок + over-fetch (limit = topK * K) + курсор.
    // TODO(arch): проверить NOT IN на ПУСТОМ exclude-списке (новый пользователь без лайков) —
    //  это первый же реальный сценарий, а не край.
    public List<UUID> selectCandidateIds(UUID userId) {
        return List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private RecommendationDTO messageToDTO(ProfileMessage pm){
        Integer age = null;
        try {
            if (!pm.getBirthDate().isBlank()) {
                age = Period.between(LocalDate.parse(pm.getBirthDate()), LocalDate.now()).getYears();
            }
        } catch (DateTimeParseException _) {
            log.error("Wrong DateTime format of birthDate in profile with id={} and userId={}", pm.getId(), pm.getUserId());
        }

        return new RecommendationDTO(pm.getUserId(),
                pm.getDisplayName(),
                pm.getId(),
                pm.getCity(),
                pm.getGender(),
                pm.getBio(),
                age);
    }
}
