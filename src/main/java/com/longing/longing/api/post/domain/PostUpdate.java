package com.longing.longing.api.post.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostUpdate {

    private String title;

    private String content;

    @Builder
    public PostUpdate(
            @JsonProperty("title") String title,
            @JsonProperty("content") String content) {
        this.title = title;
        this.content = content;
    }
}
