package com.longing.longing.comment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
public class CommentCreate {

    private final Long postId;

    @NotBlank(message = "comment should not be blank")
    private final String content;


    @Builder
    public CommentCreate(
            @JsonProperty("postId") Long postId,
            @JsonProperty("content") String content) {
        this.postId = postId;
        this.content = content;
    }
}
