package com.longing.longing.user.infrastructure;

import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.user.Provider;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(UserEntity.fromModel(user)).toModel();
    }

    @Override
    public Optional<User> findById(long id) {
        return userJpaRepository.findById(id).map(UserEntity::toModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserEntity::toModel);
    }

    @Override
    public Optional<User> findByProviderId(String providerId) {
        return userJpaRepository.findByProviderId(providerId).map(UserEntity::toModel);
    }

    @Override
    public Optional<UserEntity> findByEmailAndProvider(String email, Provider provider) {
        return userJpaRepository.findByEmailAndProvider(email, provider);
    }

    @Override
    public User getById(long id) {
        return null;
    }
}
