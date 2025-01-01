package com.longing.longing.like.infrastructure;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.like.domain.LikeCommentCreate;
import com.longing.longing.like.service.port.CommentLikeRepository;
import com.longing.longing.user.domain.User;

import java.util.Optional;

public class CommentLikeRepositoryImpl implements CommentLikeRepository {
    @Override
    public void likeComment(LikeCommentCreate likeComment) {

    }

    @Override
    public Optional<LikeCommentCreate> findByPostAndUser(Comment comment, User user) {
        return Optional.empty();
    }

    @Override
    public LikeCommentCreate save(LikeCommentCreate likeComment) {
        return null;
    }

    @Override
    public Optional<LikeCommentCreate> findById(long likeId) {
        return Optional.empty();
    }

    @Override
    public void deleteById(long likeId) {

    }
}
