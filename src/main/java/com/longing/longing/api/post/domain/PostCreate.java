package com.longing.longing.api.post.domain;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
@Builder
public class PostCreate {

    @NotBlank(message = "title should not be blank")
    private final String title;

    @NotBlank(message = "content should not be blank")
    private final String content;

//    private final List<MultipartFile> images;

//    @Builder
//    public PostCreate(@JsonProperty("title") String title,
//                      @JsonProperty("content") String content,
//                      List<MultipartFile> images) {
//        this.title = title;
//        this.content = content;
//        this.images = images;
//    }

//    @Builder
    public PostCreate(String title,
                      String content) {
        this.title = title;
        this.content = content;
//        this.images = images;
    }
}