package com.longing.longing.api.like.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeJpaRepository extends JpaRepository<PostLikeEntity, Long> {

    Optional<PostLikeEntity> findByPostIdAndUserId(long postId, long userId);

    Optional<PostLikeEntity> findById(long id);

    void deleteById(long id);

}
