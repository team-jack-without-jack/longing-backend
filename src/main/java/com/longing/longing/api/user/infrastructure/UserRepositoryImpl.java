package com.longing.longing.api.user.infrastructure;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.domain.UserBlock;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserBlockJpaRepository userBlockJpaRepository;

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
    public Optional<User> findByEmailAndProvider(String email, Provider provider) {
        return userJpaRepository.findByEmailAndProvider(email, provider).map(UserEntity::toModel);
    }

    @Override
    public void deleteById(long id) {
        userJpaRepository.deleteById(id);
    }

    @Override
    public void blockUser(User user, User blockedUser) {
        userBlockJpaRepository.save(UserBlockEntity.fromModel(user, blockedUser));
    }

    @Override
    public Optional<UserBlock> findBlockedData(User user, User blockedUser) {
        log.info("@@@@@> " + user.getId());
        log.info("#####> " + blockedUser.getId());
        return userBlockJpaRepository.findByUserIdAndBlockedUserId(user.getId(), blockedUser.getId()).map(UserBlockEntity::toModel);
    }

    @Override
    public void deleteByUserBlockedId(long userBlockId) {
        userBlockJpaRepository.deleteById(userBlockId);
    }
}
