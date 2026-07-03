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

    @GetMapping("/recommendations")
    public Mono<List<RecommendationDTO>> recommendations(@RequestParam UUID userId,
                                                         @RequestParam(defaultValue = "10") int topK) {

        return Mono.fromCallable(() -> recommendationService.recommend(userId, topK))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
