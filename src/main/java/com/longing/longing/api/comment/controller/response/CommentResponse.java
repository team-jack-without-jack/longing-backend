package com.longing.longing.api.comment.controller.response;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentResponse {

    private final Long id;
    private final String content;
    private final User user;
    private final Long postId;
    private final LocalDateTime createdDate;
    private final LocalDateTime modifiedDate;

    @Builder
    public CommentResponse(
            Long id,
            String content,
            User user,
            Long postId,
            LocalDateTime createdDate,
            LocalDateTime modifiedDate) {
        this.id = id;
        this.content = content;
        this.user = user;
        this.postId = postId;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    public static CommentResponse fromDomain(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(comment.getUser())
                .postId(comment.getPost().getId())
                .createdDate(comment.getCreatedDate())
                .modifiedDate(comment.getModifiedDate())
                .build();
    }
}
