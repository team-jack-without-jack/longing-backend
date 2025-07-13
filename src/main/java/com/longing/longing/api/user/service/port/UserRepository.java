package com.longing.longing.api.user.service.port;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.infrastructure.UserEntity;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByEmailAndProvider(String email, Provider provider);

//    User getById(long id);

    void deleteById(long id);

    void blockUser(User user, User blockedUser);
}


