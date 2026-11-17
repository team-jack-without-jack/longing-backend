package com.longing.longing.api.comment.service.port;

import com.longing.longing.api.comment.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(Long id);
    Page<Comment> findByPostId(long postId, Pageable pageable);

    List<Comment> getCommentList(long postId, long lastCommentId, Pageable pageable);
    /**
     * 아직 삭제되지 않은 댓글이면 soft delete하고 1, 이미 삭제됐거나 없으면 0을 반환한다 (#126).
     * 동시에 지우는 요청 중 하나만 1을 받는다.
     */
    int softDelete(long commentId);

    /**
     * 작성자의 삭제되지 않은 댓글이면 내용과 수정 시각을 바꾸고 1, 아니면 0을 반환한다 (#129).
     * content가 null이면 내용은 그대로 두고 수정 시각만 바꾼다.
     * 동시에 커밋된 삭제를 되돌리지 않는다. 영속성 컨텍스트를 반영 후 비운다.
     */
    int updateContent(long commentId, long userId, String content);

    Optional<Comment> findByIdAndUserId(long commentId, long id);
}
