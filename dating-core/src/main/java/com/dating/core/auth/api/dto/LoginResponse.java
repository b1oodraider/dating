package com.dating.core.auth.api.dto;

public record LoginResponse(String AccessToken, String RefreshToken) {
}
