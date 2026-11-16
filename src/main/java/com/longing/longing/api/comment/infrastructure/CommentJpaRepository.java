package com.longing.longing.api.comment.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {
    Optional<CommentEntity> findById(Long id);
    Optional<CommentEntity> findByIdAndUserId(Long id, Long userId);
    Page<CommentEntity> findByPostId(long id, Pageable pageable);
    List<CommentEntity> findByPostIdAndIdGreaterThanOrderByIdAsc(long postId, long lastCommentId, Pageable pageable);

    /**
     * 아직 삭제되지 않은 댓글만 soft delete한다. 바뀐 행 수(0 또는 1)를 반환한다 (#126).
     * 동시에 지우는 요청은 행 잠금으로 순서가 정해지고, 뒤 요청은 최신 행(deleted = true)으로 조건을 다시 평가해 0을 받는다.
     */
    @Transactional
    @Modifying
    @Query("UPDATE CommentEntity c SET c.deleted = true, c.deletedDate = CURRENT_TIMESTAMP WHERE c.id = :id AND c.deleted = false")
    int softDeleteById(@Param("id") Long id);
}
