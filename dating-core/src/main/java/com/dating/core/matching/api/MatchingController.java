package com.dating.core.matching.api;

import com.dating.core.common.security.AuthPrincipal;
import com.dating.core.matching.dto.NewLike;
import com.dating.core.matching.service.LikeMatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {
    private final LikeMatchService likeMatchService;

    public MatchingController(LikeMatchService likeMatchService) {
        this.likeMatchService = likeMatchService;
    }

    // TODO(API-баг): пара УЖЕ сматчена → setMatch ловит unique violation → вернётся {"match": false},
    //  хотя матч существует. Так ответ получит и проигравший гонку, и повторно лайкнувший.
    //  При violation надо проверять «матч уже есть?» и отвечать true (see: idempotent API).
    // TODO(REST-конвенция): глагол в URL — обычно POST /api/likes с телом, а не /matching/setLike.
    // TODO: результат setLike игнорируется — повторный лайк молча проходит; осознанно ли?
    @PostMapping("/setLike")
    public ResponseEntity<Map<String, Boolean>> setLike(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody NewLike newLike) {
        if(principal.userId().equals(newLike.toUserId())) { return ResponseEntity.status(400).body(Map.of("match", false));}
        likeMatchService.setLike(principal.userId(),newLike.toUserId());
        boolean matched = likeMatchService.setMatch(principal.userId(),newLike.toUserId());
        return ResponseEntity.status(201).body(Map.of("match", matched));
    }
}
