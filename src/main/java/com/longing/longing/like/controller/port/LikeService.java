package com.longing.longing.like.controller.port;

import com.longing.longing.like.domain.LikePost;

public interface LikeService {
    void likePost(LikePost likePost);
    void unlikePost(long likeId);
}
