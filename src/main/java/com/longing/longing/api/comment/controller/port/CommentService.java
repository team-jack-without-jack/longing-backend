package com.longing.longing.api.comment.controller.port;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.config.auth.dto.CustomUserDetails;

import java.util.List;
import java.util.Optional;

public interface CommentService {
    Comment createComment(User user, CommentCreate commentCreate, String clientRequestId);

    Optional<Comment> findByClientRequestId(long userId, String clientRequestId);
    Comment updateComment(User user, long commentId, CommentUpdate commentUpdate);

    List<Comment> getCommentList(long postId, long lastCommentId, int limit);

    void deleteComment(User user, long commentId);
}
