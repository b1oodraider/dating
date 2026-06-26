package com.dating.core.common.security;

import java.util.UUID;

/**
 * Аутентифицированный пользователь в {@code SecurityContext}.
 * Кладётся в principal вместо голого UUID, чтобы корректно
 * доставаться через {@code @AuthenticationPrincipal} в контроллерах.
 */
public record AuthPrincipal(UUID userId, String role) {
}