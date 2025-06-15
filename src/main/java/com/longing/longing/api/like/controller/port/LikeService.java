package com.longing.longing.api.like.controller.port;

import com.longing.longing.api.like.domain.LikePostCreate;
import com.longing.longing.api.like.domain.LikePostDelete;

public interface LikeService {
    void likePost(LikePostCreate likePostCreate);
    void unlikePost(LikePostDelete likePostDelete);

}
