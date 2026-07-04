package com.dating.core.matching.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NewLike(@NotNull UUID toUserId) {
}
