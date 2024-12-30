package com.longing.longing.comment.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CommentCreate {

    private final String content;

    private final Long userId;

    private final Long postId;

    @Builder
    public CommentCreate(String content, Long userId, Long postId) {
        this.content = content;
        this.userId = userId;
        this.postId = postId;
    }
}
