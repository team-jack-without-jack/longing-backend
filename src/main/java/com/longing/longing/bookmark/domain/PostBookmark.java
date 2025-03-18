package com.longing.longing.bookmark.domain;

import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostBookmark {
    private final Long id;
    private final Post post;
    private final User user;

    @Builder
    public PostBookmark(Long id, Post post, User user) {
        this.id = id ;
        this.post = post;
        this.user = user;
    }
}

