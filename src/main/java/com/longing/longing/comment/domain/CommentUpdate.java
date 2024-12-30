package com.longing.longing.comment.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CommentUpdate {
    private String content;

    @Builder
    public CommentUpdate(String content) {
        this.content = content;
    }
}
