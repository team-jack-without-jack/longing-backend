package com.longing.longing.comment.controller.port;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.domain.CommentCreate;
import com.longing.longing.comment.domain.CommentUpdate;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CommentService {
    Comment createComment(String oauthId, CommentCreate commentCreate);
    Comment updateComment(long commentId, CommentUpdate commentUpdate);

    Page<Comment> getCommentList(long postId, int page, int size, String sortBy, String sortDirection);

    void deletePost(long commentId);
}
