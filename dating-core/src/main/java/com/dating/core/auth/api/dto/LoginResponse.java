package com.dating.core.auth.api.dto;

public record LoginResponse(String accessToken, String refreshToken) {
}
