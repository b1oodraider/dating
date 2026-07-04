package com.dating.matching.dto;

// TODO: поле id на самом деле содержит userId (см. RecommendationService.messageToDTO) —
//  переименовать в userId, иначе клиент API решит, что это id рекомендации/профиля.
public record RecommendationDTO(String userId,
                                String displayName,
                                String profileId,
                                String city,
                                String gender,
                                String bio,
                                Integer age) {
}
