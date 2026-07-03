package com.dating.matching.dto;

public record RecommendationDTO(String id,
                                String displayName,
                                String profileId,
                                String city,
                                String gender,
                                String bio,
                                Integer age) {
}
