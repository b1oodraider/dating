package com.dating.core.profile.service;

import org.springframework.modulith.NamedInterface;

import java.util.UUID;

/**
 * Узкий интерфейс для создания начального профиля при регистрации.
 * Это публичный API модуля {@code profile} для модуля {@code auth} —
 * единственная разрешённая точка связи между ними.
 */
@NamedInterface
public interface ProfileCreator {

    void createInitialProfile(UUID userId, String displayName);
}