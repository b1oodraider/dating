package com.dating.core.matching.api;

import com.dating.core.common.security.AuthPrincipal;
import com.dating.core.matching.dto.NewLike;
import com.dating.core.matching.service.LikeMatchService;
import io.grpc.Status;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    // TODO(REST-конвенция): глагол в URL — обычно POST /api/likes с телом, а не /matching/setLike.
    // TODO(bug): 400 при self-like отдаётся телом успеха {"match": false} — вернуть ProblemDetail.
    // TODO(security): toUserId не проверяется на существование (FK нет by design) — лайк произвольного
    //  UUID создаёт строку в likes, при взаимности — матч и MatchCreated в Kafka. Нужна проверка
    //  существования профиля + блок-лист + суточная квота лайков (IP-лимита на gateway мало).
    // TODO(bug): setMatch вызывается даже когда setLike вернул false (дубль лайка) — поведение
    //  похоже на осознанную идемпотентность, но нигде не зафиксировано.
    // TODO(debt): неиспользуемый import io.grpc.Status тянет gRPC-тип в web-слой модуля.
    @PostMapping("/setLike")
    public ResponseEntity<Map<String, Boolean>> setLike(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody NewLike newLike) {
        if(principal.userId().equals(newLike.toUserId())) { return ResponseEntity.status(400).body(Map.of("match", false));}
        var status = 201;
        if(!likeMatchService.setLike(principal.userId(),newLike.toUserId())){
            status = HttpStatus.OK.value();
        }
        boolean matched = likeMatchService.setMatch(principal.userId(),newLike.toUserId());
        return ResponseEntity.status(status).body(Map.of("match", matched));
    }
}
