package com.longing.longing.post.domain;

import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostCreate {

    private final String title;

    private final String content;

    private final User user;

    @Builder
    public PostCreate(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
    }
}