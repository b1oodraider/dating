package com.dating.core.profile.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank String displayName,
        LocalDate birthDate,
        String gender,
        String bio,
        String city
) {}