package com.longing.longing.comment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
public class CommentCreate {

    private final Long postId;

    @NotBlank(message = "comment should not be blank")
    @Size(max = 1000, message = "comment length limit")
    private final String content;


    @Builder
    public CommentCreate(
            @JsonProperty("postId") Long postId,
            @JsonProperty("content") String content) {
        this.postId = postId;
        this.content = content;
    }
}
