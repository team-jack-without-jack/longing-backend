package com.longing.longing.comment.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {
    Optional<CommentEntity> findById(Long id);
    Optional<CommentEntity> findByIdAndUserId(Long id, Long userId);
    Page<CommentEntity> findByPostId(long id, Pageable pageable);

    List<CommentEntity> findByPostIdAndIdGreaterThanOrderByIdAsc(long postId, long lastCommentId, Pageable pageable);

    void deleteById(long id);
}
