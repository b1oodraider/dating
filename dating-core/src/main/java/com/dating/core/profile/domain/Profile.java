package com.dating.core.profile.domain;

import com.dating.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Анкета пользователя. Связана с {@code User} по {@code userId} (1:1),
 * но без JPA-ассоциации между модулями — связь только по идентификатору,
 * чтобы границы модулей оставались чистыми (важно для будущего распила).
 */

@Entity
@Table(name="profiles")
public class Profile extends BaseEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column
    private String gender;

    @Column
    private String bio;

    @Column
    private String city;

    public Profile() {}

    public Profile(UUID userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    /** Обновляет редактируемые поля анкеты. */
    public void update(ProfileUpdate data) {
        this.displayName = data.displayName();
        this.birthDate = data.birthDate();
        this.gender = data.gender();
        this.bio = data.bio();
        this.city = data.city();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public String getBio() {
        return bio;
    }

    public String getCity() {
        return city;
    }
}
