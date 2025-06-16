package com.longing.longing.api.like.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LikePostCreate {

//    private final Long id;
//    private final Post post;
//    private final User user;
    private final Long postId;
    private final Long userId;

    @Builder
    public LikePostCreate(Long postId, Long userId) {
//        this.id = id;
        this.postId = postId;
        this.userId = userId;
    }
}
