package com.dating.matching.api;

import com.dating.matching.dto.RecommendationDTO;
import com.dating.matching.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // TODO(security): сервис ничем не защищён и не стоит за gateway — любой может запросить
    //  рекомендации для чужого userId. Минимум: маршрут в gateway + userId из JWT, а не из параметра.
    // TODO: валидация topK (отрицательное/огромное значение сейчас проходит молча;
    //  limit(-1) кинет IllegalArgumentException -> 500).
    @GetMapping("/recommendations")
    public Mono<List<RecommendationDTO>> recommendations(@RequestParam UUID userId,
                                                         @RequestParam(defaultValue = "10") int topK) {

        return Mono.fromCallable(() -> recommendationService.recommend(userId, topK))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
