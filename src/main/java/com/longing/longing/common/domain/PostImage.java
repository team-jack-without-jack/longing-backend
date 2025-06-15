package com.longing.longing.common.domain;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PostImage {
    private Long id;
    private String address;
    private Post post;
    private User user;

    @Builder
    public PostImage(Long id, String address, Post post, User user) {
        this.id = id;
        this.address = address;
        this.post = post;
        this.user = user;
    }

    public static PostImage from(String address, Post post, User user) {
        return PostImage.builder()
                .address(address)
                .post(post)
                .user(user)
                .build();
    }
}
