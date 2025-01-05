package com.longing.longing.comment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CommentUpdate {
    private String content;

    @Builder
    public CommentUpdate(@JsonProperty("content") String content) {
        this.content = content;
    }
}
