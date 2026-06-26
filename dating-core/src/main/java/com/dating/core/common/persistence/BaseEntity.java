package com.dating.core.common.persistence;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Базовый класс сущностей с автоматическим аудитом времени.
 *
 * <p>Поля {@code createdAt} и {@code updatedAt} заполняет Spring Data JPA
 * через {@link AuditingEntityListener}; вручную их выставлять не нужно.
 * Требует {@code @EnableJpaAuditing} на конфигурации приложения.
 */

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
