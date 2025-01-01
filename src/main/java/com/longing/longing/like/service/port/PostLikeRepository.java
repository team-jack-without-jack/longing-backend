package com.longing.longing.like.service.port;

import com.longing.longing.like.domain.LikePostCreate;
import com.longing.longing.like.domain.PostLike;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;

import java.util.Optional;

public interface PostLikeRepository {

    void likePost(LikePostCreate likePost);
    Optional<PostLike> findByPostAndUser(Post post, User user);
    PostLike save(LikePostCreate likePostCreate, Post post, User user);

    Optional<PostLike> findById(long likeId);
    void deleteById(long likeId);
}
