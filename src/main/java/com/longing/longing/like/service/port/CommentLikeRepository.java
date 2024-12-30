package com.longing.longing.like.service.port;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.like.domain.LikeComment;
import com.longing.longing.like.domain.LikePost;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;

import java.util.Optional;

public interface CommentLikeRepository {

    void likeComment(LikeComment likeComment);

    Optional<LikeComment> findByPostAndUser(Comment comment, User user);

    LikeComment save(LikeComment likeComment);

    Optional<LikeComment> findById(long likeId);

    void deleteById(long likeId);

}
