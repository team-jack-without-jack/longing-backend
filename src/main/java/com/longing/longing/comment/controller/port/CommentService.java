package com.longing.longing.comment.controller.port;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.domain.CommentCreate;
import com.longing.longing.comment.domain.CommentUpdate;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CommentService {
    Comment createComment(String oauthId, CommentCreate commentCreate);
    Comment updateComment(String oauthId, long commentId, CommentUpdate commentUpdate);

    List<Comment> getCommentList(long postId, long lastCommentId, int limit);

    void deleteComment(String oauthId, long commentId);
}
