package com.longing.longing.post.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Tolerate;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.util.List;

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