package com.longing.longing.bookmark.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkJpaRepository extends JpaRepository<PostBookmarkEntity, Long> {
    Optional<PostBookmarkEntity> findByPostIdAndUserId(Long postId, Long userId);
}
