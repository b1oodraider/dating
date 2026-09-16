package com.dating.core.profile.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

// TODO(security): birthDate без @Past и без проверки 18+ — дейтинг-сервис принимает
//  несовершеннолетних и даты из будущего (отрицательный возраст ломает ранжирование).
// TODO(bug): нет @Size под длины колонок (display_name 100, gender 16, city 255), нет
//  whitelist/enum для gender; bio (TEXT) не ограничен вообще.
public record UpdateProfileRequest(
        @NotBlank String displayName,
        LocalDate birthDate,
        String gender,
        String bio,
        String city
) {}