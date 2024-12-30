package com.longing.longing.like.controller;

import com.longing.longing.like.controller.port.LikeService;
import com.longing.longing.like.domain.LikePost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/")
    public void likePost(
            @RequestBody LikePost likePost
    ) {
        likeService.likePost(likePost);
    }
}
