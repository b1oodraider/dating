package com.dating.core.matching.domain;

import com.dating.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name="likes")
public class Like extends BaseEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name="from_user_id", nullable = false)
    private UUID fromUserId;

    @Column(name="to_user_id", nullable = false)
    private UUID toUserId;

    public Like(UUID fromUserId, UUID toUserId) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
    }

    public Like() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromUserId() {
        return fromUserId;
    }

    public UUID getToUserId() {
        return toUserId;
    }

    public void setFromUserId(UUID from_user_id) {
        this.fromUserId = from_user_id;
    }

    public void setToUserId(UUID to_user_id) {
        this.toUserId = to_user_id;
    }
}
