package com.longing.longing.api.comment.domain;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Comment {

    private Long id;

    private String content;

    private Post post;

    private User user;

    @Builder
    public Comment(Long id, String content, Post post, User user) {
        this.id = id;
        this.content = content;
        this.post = post;
        this.user = user;
    }

    public static Comment from(User user, Post post, CommentCreate commentCreate) {
        return Comment.builder()
                .content(commentCreate.getContent())
                .post(post)
                .user(user)
                .build();
    }

    public Comment update(CommentUpdate commentUpdate) {
        if (commentUpdate.getContent() != null) {
            this.content = commentUpdate.getContent();
        }
        return this;
//        return Comment.builder()
//                .content(commentUpdate.getContent())
//                .build();
    }
}
