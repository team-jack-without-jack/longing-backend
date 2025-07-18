package com.longing.longing.api.like.service.port;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.like.domain.PostLike;
import com.longing.longing.api.user.domain.User;

import java.util.Optional;

public interface PostLikeRepository {

    Optional<PostLike> findByPostAndUser(Post post, User user);
    void save(PostLike postLike);

    void deleteById(long likeId);
}
