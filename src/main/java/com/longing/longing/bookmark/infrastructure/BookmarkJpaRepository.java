package com.longing.longing.bookmark.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BookmarkJpaRepository extends JpaRepository<PostBookmarkEntity, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @Transactional
    @Modifying
    @Query("DELETE FROM PostBookmarkEntity b WHERE b.user.id = :userId AND b.post.id = :postId")
    int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    List<PostBookmarkEntity> findByUserId(Long userId);
}
