package com.longing.longing.like.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LikePostDelete {
    private final Long postId;
    private final Long userId;

    @Builder
    public LikePostDelete(Long postId, Long userId) {
//        this.id = id;
        this.postId = postId;
        this.userId = userId;
    }
}
