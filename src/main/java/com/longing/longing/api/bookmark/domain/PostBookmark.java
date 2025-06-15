package com.longing.longing.api.bookmark.domain;

import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostBookmark {
    private final Long id;
    private final Post post;
    private final User user;

    @Builder
    public PostBookmark(Long id, Post post, User user) {
        this.id = id;
        this.post = post;
        this.user = user;
    }
}

