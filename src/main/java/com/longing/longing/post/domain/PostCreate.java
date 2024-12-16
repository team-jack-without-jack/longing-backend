package com.longing.longing.post.domain;

import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostCreate {

    private final String title;

    private final String content;

    private final Long userId;


    @Builder
    public PostCreate(Long userId, String title, String content) {
        this.title = title;
        this.content = content;
        this.userId = userId;
    }
}