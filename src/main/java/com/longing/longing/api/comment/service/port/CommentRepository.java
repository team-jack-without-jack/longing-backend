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
    void deleteById(long commentId);

    Optional<Comment> findByIdAndUserId(long commentId, long id);
}
