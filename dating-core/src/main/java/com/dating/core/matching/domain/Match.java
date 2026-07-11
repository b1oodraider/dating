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

    @Column(nullable = false, name="user_low")
    private UUID userLow;

    @Column(nullable = false, name="user_high")
    private UUID userHigh;

    public Match(UUID userLow, UUID userHigh) {
        if(userLow.compareTo(userHigh) >= 0) {
            this.userLow = userHigh;
            this.userHigh = userLow;
        }else {
            this.userLow = userLow;
            this.userHigh = userHigh;
        }
    }

    public Match() {}

    public UUID getId() {
        return id;
    }

    public UUID getUserLow() {
        return userLow;
    }

    public UUID getUserHigh() {
        return userHigh;
    }
}
