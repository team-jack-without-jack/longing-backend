package com.longing.longing.like.service.port;

import com.longing.longing.like.domain.LikePostCreate;
import com.longing.longing.like.domain.PostLike;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;

import java.util.Optional;

public interface PostLikeRepository {

    Optional<PostLike> findByPostAndUser(Post post, User user);
    void save(PostLike postLike);

    Optional<PostLike> findById(long likeId);
    void deleteById(long likeId);
}
