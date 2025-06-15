package com.longing.longing.api.comment.controller.port;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.config.auth.dto.CustomUserDetails;

import java.util.List;

public interface CommentService {
    Comment createComment(CustomUserDetails userDetails, CommentCreate commentCreate);
    Comment updateComment(CustomUserDetails userDetails, long commentId, CommentUpdate commentUpdate);

    List<Comment> getCommentList(long postId, long lastCommentId, int limit);

    void deleteComment(CustomUserDetails userDetails, long commentId);
}
