package com.longing.longing.api.user.infrastructure;

import com.longing.longing.api.user.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findById(long id);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByProviderId(String providerId);

    Optional<UserEntity> findByEmailAndProvider(String email, Provider provider);
}
