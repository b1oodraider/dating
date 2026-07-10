package com.dating.api_gateway.common.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;


@RestController
@RequestMapping("/fallback")
public class FallbackController {


    @RequestMapping("/core")
    public Mono<ResponseEntity<Map<String,String>>> coreFallback() {
        var response = Map.of("error", "Service unavailable", "message", "сервис временно недоступен");
        return Mono.just(ResponseEntity.status(503).body(response));
    }

    // Здесь и в application просто копии пока не углублюсь для уникальных настроек
    @RequestMapping("/matching")
    public Mono<ResponseEntity<Map<String,String>>> matchingFallback() {
        var response = Map.of("error", "Service unavailable", "message", "сервис временно недоступен");
        return Mono.just(ResponseEntity.status(503).body(response));
    }

}
