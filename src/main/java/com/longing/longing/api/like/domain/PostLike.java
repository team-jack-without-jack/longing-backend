package com.longing.longing.api.like.domain;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostLike {
    private final Long id;
    private final Post post;
    private final User user;

    @Builder
    public PostLike(Long id, Post post, User user) {
        this.id = id ;
        this.post = post;
        this.user = user;
    }
}
