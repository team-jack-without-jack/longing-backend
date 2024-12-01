package com.longing.longing.post.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PostUpdate {

    private String title;

    private String content;

    @Builder
    public PostUpdate(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
