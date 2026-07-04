package com.dating.core.auth.domain;


import com.dating.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh-токен пользователя. Хранится <b>хэш</b> токена, не сам токен,
 * чтобы утечка БД не раскрыла действующие токены.
 */
@Entity
@Table(name="refresh_tokens")
public class RefreshToken extends BaseEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name="user_id", nullable = false)
    private UUID userId;

    @Column(name="token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    public RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public RefreshToken() {}

    /** Помечает токен отозванным (при logout или ротации). */
    public void revoke() {
        this.revoked = true;
    }

    public boolean isActive() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revoked;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
