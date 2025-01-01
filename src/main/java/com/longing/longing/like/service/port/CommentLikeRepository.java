package com.longing.longing.like.service.port;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.like.domain.LikeCommentCreate;
import com.longing.longing.user.domain.User;

import java.util.Optional;

public interface CommentLikeRepository {

    void likeComment(LikeCommentCreate likeComment);

    Optional<LikeCommentCreate> findByPostAndUser(Comment comment, User user);

    LikeCommentCreate save(LikeCommentCreate likeComment);

    Optional<LikeCommentCreate> findById(long likeId);

    void deleteById(long likeId);

}
