package com.dating.core.profile.api;


import com.dating.core.common.security.AuthPrincipal;
import com.dating.core.profile.api.dto.ProfileResponse;
import com.dating.core.profile.api.dto.UpdateProfileRequest;
import com.dating.core.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


/**
 * REST-эндпоинты профилей. Текущий пользователь определяется по JWT
 * (principal выставил {@code JwtAuthFilter}).
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** Свой профиль. {@code userId} берётся из токена, не из URL. */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(profileService.getByUserId(principal.userId()));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        return ResponseEntity.ok(profileService.update(principal.userId(), updateProfileRequest));
    }

    /** Чужой профиль по id. */
    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID profileId) {
        return ResponseEntity.ok(profileService.getById(profileId));
    }

}
