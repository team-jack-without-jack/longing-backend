package com.longing.longing.bookmark.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkJpaRepository extends JpaRepository<PostBookmarkEntity, Long> {
    Optional<PostBookmarkEntity> findByPostIdAndUserId(Long postId, Long userId);
    List<PostBookmarkEntity> findByUserId(Long userId);
}
