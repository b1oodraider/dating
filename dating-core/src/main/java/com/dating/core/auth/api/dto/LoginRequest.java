package com.dating.core.auth.api.dto;

// TODO(bug): @Valid в AuthController стоит, а констрейнтов нет — {"email":null,"password":null}
//  доезжает до сервиса; при существующем email null-пароль уходит в BCrypt.matches (риск 500).
//  Добавить @NotBlank @Email на email и @NotBlank @Size(max=72) на password.
public record LoginRequest(String email, String password) {
}
