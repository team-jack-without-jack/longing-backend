package com.longing.longing.like.domain;

import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LikePost {

    private final Long id;
    private final Post post;
    private final User user;

    @Builder
    public LikePost(Long id, Post post, User user) {
        this.id = id;
        this.post = post;
        this.user = user;
    }
}
