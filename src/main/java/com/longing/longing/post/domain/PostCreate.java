package com.longing.longing.post.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostCreate {

    private final String title;

    private final String content;


    @Builder
    public PostCreate(@JsonProperty("title") String title,
                      @JsonProperty("content") String content) {
        this.title = title;
        this.content = content;
    }
}