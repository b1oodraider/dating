package com.dating.core.profile.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String displayName,
        LocalDate birthDate,
        String gender,
        String bio,
        String city
) {}