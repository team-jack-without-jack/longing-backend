package com.longing.longing.api.user.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserBlockJpaRepository extends JpaRepository<UserBlockEntity, Long> {

    boolean existsByUserIdAndBlockedUserId(long userId, long blockedUserId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserBlockEntity b WHERE b.user.id = :userId AND b.blockedUser.id = :blockedUserId")
    int deleteByUserIdAndBlockedUserId(@Param("userId") long userId, @Param("blockedUserId") long blockedUserId);
}
