package com.longing.longing.like.service.port;

import com.longing.longing.like.domain.LikePost;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;

import java.util.Optional;

public interface PostLikeRepository {

    void likePost(LikePost likePost);
    Optional<LikePost> findByPostAndUser(Post post, User user);
    LikePost save(LikePost likePost);

    Optional<LikePost> findById(long likeId);
    void deleteById(long likeId);
}
