package com.longing.longing.api.comment.domain;

import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CommentResponse {

    private final Long id;
    private final String content;
    private final User user;
    private final Long postId;

    @Builder
    public CommentResponse(Long id, String content, User user, Long postId) {
        this.id = id;
        this.content = content;
        this.user = user;
        this.postId = postId;
    }

    public static CommentResponse fromDomain(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(comment.getUser())
                .postId(comment.getPost().getId())
                .build();
    }
}
