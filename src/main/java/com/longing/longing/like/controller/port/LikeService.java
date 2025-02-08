package com.longing.longing.like.controller.port;

import com.longing.longing.like.domain.LikePostCreate;
import com.longing.longing.like.domain.LikePostDelete;

public interface LikeService {
    void likePost(LikePostCreate likePostCreate);
    void unlikePost(LikePostDelete likePostDelete);

}
