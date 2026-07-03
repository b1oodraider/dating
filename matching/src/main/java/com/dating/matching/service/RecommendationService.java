package com.dating.matching.service;

import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.matching.dto.RecommendationDTO;
import com.dating.matching.dto.Criteria;
import com.dating.matching.service.records.Scored;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationService {

    private final CandidateProfileFetcher fetcher;
    private final RankingService rankings;

    public RecommendationService(CandidateProfileFetcher fetcher, RankingService rankings) {
        this.fetcher = fetcher;
        this.rankings = rankings;
    }

    public List<RecommendationDTO> recommend(UUID userId, int topK) {
        List<UUID> recommendedProfiles = selectCandidateIds(userId);
        List<ProfileMessage> profiles = fetcher.fetchCandidateProfiles(recommendedProfiles);
        // TODO: критерии из профиля запросившего (GetProfileByUserId gRPC) — сейчас заглушка
        Criteria me = new Criteria(28, "Moscow", "female");
        return profiles.stream()
                .map(p-> new Scored(p, rankings.score(p, me)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(topK)
                .map(Scored::profile)
                .map(this::messageToDTO)
                .toList();
    }

    // TODO: реальная выборка кандидатов (Neo4j/фильтры) — вне 18-дневного спринта
    public List<UUID> selectCandidateIds(UUID userId) {
        return List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private RecommendationDTO messageToDTO(ProfileMessage pm){
        Integer age = null;
        try {
            if (!pm.getBirthDate().isBlank()) {
                age = Period.between(LocalDate.parse(pm.getBirthDate()), LocalDate.now()).getYears();
            }
            //TODO: добавить лог с ошибкой
        } catch (DateTimeParseException _) {
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
