package com.longing.longing.user.service.port;

import com.longing.longing.user.Provider;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    Optional<UserEntity> findByEmailAndProvider(String email, Provider provider);

    User getById(long id);

}


