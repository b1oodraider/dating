package com.dating.core.matching.domain;

import com.dating.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name="matches")
public class Match extends BaseEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private UUID user_low;

    @Column(nullable = false)
    private UUID user_high;

    public Match(UUID user_low, UUID user_high) {
        if(user_low.compareTo(user_high) >= 0) {
            this.user_low = user_high;
            this.user_high = user_low;
        }else {
            this.user_low = user_low;
            this.user_high = user_high;
        }
    }

    public Match() {}

    public UUID getId() {
        return id;
    }

    public UUID getUser_low() {
        return user_low;
    }

    public UUID getUser_high() {
        return user_high;
    }
}
