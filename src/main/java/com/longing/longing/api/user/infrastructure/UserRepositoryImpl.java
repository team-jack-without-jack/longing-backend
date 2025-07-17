package com.longing.longing.api.user.infrastructure;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.domain.UserBlock;
import com.longing.longing.api.user.service.port.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserBlockJpaRepository userBlockJpaRepository;
    private final JPAQueryFactory queryFactory;

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
        return userBlockJpaRepository.findByUserIdAndBlockedUserId(user.getId(), blockedUser.getId()).map(UserBlockEntity::toModel);
    }

    @Override
    public void deleteByUserBlockedId(long userBlockId) {
        userBlockJpaRepository.deleteById(userBlockId);
    }

    @Override
    public Page<User> findBlockedUserList(Long id, String keyword, Pageable pageable) {
//        QUserBlockEntity userBlock = QUserBlockEntity.userBlockEntity;
//
//        // 1) where, offset/limit, orderBy, fetchResults()
//        List<UserBlockEntity> qr = queryFactory
//                .select(userBlock)
//                .from(userBlock)
//                .where(
//                        userBlock.user.id.eq(id),
//                        // keyword가 있을 때만 검색 조건 추가
//                        keyword != null && !keyword.isEmpty()
//                                ? userBlock.blockedUser.name.containsIgnoreCase(keyword)
//                                : null
//                )
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .orderBy(userBlock.createdDate.desc())
//                .fetch();
//
//        List<UserBlock> results = qr.stream()
//                .map(entity -> UserBlock.builder()
//                        .user(entity.getUser().toModel())
//                        .blockedUser(entity.getBlockedUser().toModel())
//                        .build()
//                ).collect(Collectors.toList());
//
//        int total = queryFactory
//                .select(userBlock)
//                .from(userBlock)
//                .where(
//                        userBlock.user.id.eq(id),
//                        // keyword가 있을 때만 검색 조건 추가
//                        keyword != null && !keyword.isEmpty()
//                                ? userBlock.blockedUser.name.containsIgnoreCase(keyword)
//                                : null
//                )
//                .fetch().size();
//
//        // 3) PageImpl로 감싸기
//        return new PageImpl<>(results, pageable, total);

        QUserBlockEntity ub = QUserBlockEntity.userBlockEntity;
        QUserEntity blocked = new QUserEntity("blockedUser");

        List<UserBlockEntity> qr = queryFactory
                .select(ub)
                .from(ub)
                .innerJoin(ub.blockedUser, blocked).fetchJoin()
                .where(
                        ub.user.id.eq(id),
                        (keyword != null && !keyword.isEmpty())
                                ? blocked.name.containsIgnoreCase(keyword)
                                : null
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(ub.createdDate.desc())
                .fetch();

        int total = queryFactory
                .select(ub)
                .from(ub)
                .innerJoin(ub.blockedUser, blocked).fetchJoin()
                .where(
                        ub.user.id.eq(id),
                        (keyword != null && !keyword.isEmpty())
                                ? blocked.name.containsIgnoreCase(keyword)
                                : null
                )
                .fetch().size();

        List<User> content = qr.stream()
                .map(entity -> entity.getBlockedUser().toModel())
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, total);

    }
}
