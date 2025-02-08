package com.longing.longing.comment.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class CommentResponse {

    private final Long id;
    private final String content;
    private final Long userId;
    private final Long postId;

    @Builder
    public CommentResponse(Long id, String content, Long userId, Long postId) {
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.postId = postId;
    }

    public static CommentResponse fromDomain(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .postId(comment.getPost().getId())
                .build();
    }
}
