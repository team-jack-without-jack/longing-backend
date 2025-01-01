package com.longing.longing.like.domain;

import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
