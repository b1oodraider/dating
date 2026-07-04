package com.dating.core.matching.repo;

import com.dating.core.matching.domain.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {
    boolean existsByFromUserIdAndToUserId(UUID fromUserId, UUID toUserId);
}
