package com.longing.longing.api.comment.domain;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Comment {

    private Long id;

    private String content;

    private Post post;

    private User user;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    @JsonIgnore
    private String clientRequestId;

    @Builder
    public Comment(Long id,
                   String content,
                   Post post,
                   User user,
                   LocalDateTime createdDate,
                   LocalDateTime modifiedDate,
                   String clientRequestId) {
        this.id = id;
        this.content = content;
        this.post = post;
        this.user = user;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.clientRequestId = clientRequestId;
    }

    public static Comment from(User user, Post post, CommentCreate commentCreate) {
        return from(user, post, commentCreate, null);
    }

    public static Comment from(User user, Post post, CommentCreate commentCreate, String clientRequestId) {
        return Comment.builder()
                .content(commentCreate.getContent())
                .post(post)
                .user(user)
                .clientRequestId(clientRequestId)
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
