package com.longing.longing.post.domain;

import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Post {

    private Long id;

    private String title;

    private String content;

    private User user;

    @Builder
    public Post(Long id, String title, String content, User user) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
    }

    public static Post from(User user, PostCreate postCreate) {
        return Post.builder()
                .title(postCreate.getTitle())
                .content(postCreate.getContent())
                .user(user)
                .build();
    }

    public Post update(PostUpdate postUpdate) {
        return Post.builder()
                .title(postUpdate.getTitle())
                .content(postUpdate.getContent())
                .build();
    }
}
