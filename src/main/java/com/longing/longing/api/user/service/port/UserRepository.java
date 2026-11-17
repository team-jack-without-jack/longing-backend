package com.longing.longing.api.user.service.port;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByEmailAndProvider(String email, Provider provider);

//    User getById(long id);

    void deleteById(long id);

    boolean isBlocked(long userId, long blockedUserId);

    /**
     * 차단했으면 true, 이미 차단돼 있으면 false (#124).
     * 동시 요청은 unique 제약이 한 행만 남긴다. 트랜잭션 밖에서 호출해야 한다.
     * 위반으로 롤백되면 요청의 EntityManager(open-in-view)가 clear되어, 호출 전에 읽은 엔티티는 준영속이 된다.
     */
    boolean blockIfAbsent(User user, User blockedUser);

    /** 해당 차단을 모두 지우고 지운 행 수를 반환한다. */
    int unblock(long userId, long blockedUserId);

    Page<User> findBlockedUserList(Long id, String keyword, Pageable pageable);
}


