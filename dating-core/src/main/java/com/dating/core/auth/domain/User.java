package com.dating.core.auth.domain;

import com.dating.core.common.persistence.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;


import java.util.UUID;

/**
 * Пользователь системы: учётные данные и роль.
 * Профиль (анкета) вынесен в отдельную сущность модуля {@code profile}.
 */

@Entity
@Table(name="users")
public class User extends BaseEntity {
    @Id()
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(unique=true, nullable = false)
    private String email;

    @Column(name="password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    /**
     * Создаёт нового активного пользователя с ролью {@link Role#USER}.
     *
     * @param email        уникальный email
     * @param passwordHash уже захэшированный пароль (BCrypt), не сырой
     */
    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
    }

    public User() {
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }
}
