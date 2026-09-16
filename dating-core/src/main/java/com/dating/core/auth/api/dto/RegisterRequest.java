package com.dating.core.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// TODO(bug): нет @Size(max) под длины колонок (email 255, display_name 100) — перелив даёт
//  DataIntegrityViolationException на коммите и 500 вместо 400.
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Пароль не короче 8 символов") String password,
        @NotBlank String displayName
) {}
