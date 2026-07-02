package com.dating.core.auth.repo;

import com.dating.core.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    /**
     * Bulk-удаление просроченных токенов одним DELETE-запросом,
     * минуя persistence context (derived-метод deleteBy... сначала
     * загрузил бы все сущности и удалял по одной).
     */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :moment")
    int deleteAllExpiredBefore(@Param("moment") Instant moment);

}
