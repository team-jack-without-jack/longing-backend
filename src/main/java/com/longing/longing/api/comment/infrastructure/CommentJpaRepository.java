package com.longing.longing.api.comment.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * 작성자의 삭제되지 않은 댓글이면 내용과 수정 시각을 바꾸고 1, 아니면 0을 반환한다 (#129).
     * content가 null이면 내용은 DB의 현재 값을 그대로 두고 수정 시각만 바꾼다.
     * 엔티티 전체를 다시 저장(merge)하면 그사이 커밋된 삭제(deleted)를 되돌리므로 바꿀 컬럼만 쓴다.
     * 벌크 UPDATE라 감사(@LastModifiedDate)와 @Where를 거치지 않으므로 수정 시각과 deleted 조건을 직접 넣는다.
     * flushAutomatically: 영속성 컨텍스트에 남은 변경을 먼저 DB에 반영해, 비울 때 사라지지 않게 한다.
     * clearAutomatically: 옛 댓글 엔티티를 비워서, 이어서 다시 조회할 때 바뀐 값을 읽게 한다.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE CommentEntity c SET c.content = COALESCE(:content, c.content), c.modifiedDate = :now "
            + "WHERE c.id = :id AND c.user.id = :userId AND c.deleted = false")
    int updateContent(@Param("id") Long id, @Param("userId") Long userId, @Param("content") String content,
                      @Param("now") LocalDateTime now);
}
