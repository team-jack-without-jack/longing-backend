package com.longing.longing.api.user.infrastructure;

import com.longing.longing.api.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBlockJpaRepository extends JpaRepository<UserBlockEntity, Long> {
    Optional<UserBlockEntity> findByUserIdAndBlockedUserId(long userId, long blockedUserId);
}
