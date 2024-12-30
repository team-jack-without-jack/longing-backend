package com.longing.longing.like.infrastructure;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.like.domain.LikeComment;
import com.longing.longing.like.service.port.CommentLikeRepository;
import com.longing.longing.user.domain.User;

import java.util.Optional;

public class CommentLikeRepositoryImpl implements CommentLikeRepository {
    @Override
    public void likeComment(LikeComment likeComment) {

    }

    @Override
    public Optional<LikeComment> findByPostAndUser(Comment comment, User user) {
        return Optional.empty();
    }

    @Override
    public LikeComment save(LikeComment likeComment) {
        return null;
    }

    @Override
    public Optional<LikeComment> findById(long likeId) {
        return Optional.empty();
    }

    @Override
    public void deleteById(long likeId) {

    }
}
