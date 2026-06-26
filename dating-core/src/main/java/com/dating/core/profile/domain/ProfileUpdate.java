package com.dating.core.profile.domain;

import java.time.LocalDate;

/**
 * Доменная команда обновления анкеты. Намеренно живёт в пакете {@code domain},
 * а не {@code api.dto}: сущность не должна зависеть от слоя API.
 * Маппинг из {@code UpdateProfileRequest} (DTO) делает сервис.
 */
public record ProfileUpdate(
        String displayName,
        LocalDate birthDate,
        String gender,
        String bio,
        String city
) {
}
