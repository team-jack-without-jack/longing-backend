package com.longing.longing.post.controller;

import com.longing.longing.post.controller.port.PostService;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     * @param postCreate
     * @param authentication
     * @return
     */
    @PostMapping("/api/post")
    public ResponseEntity<Post> CreatePost(
            @RequestBody PostCreate postCreate,
            Authentication authentication) {
        String oauthId= authentication.getName(); // 현재 인증된 사용자의 ID를 가져옴
        log.info("userId>> " + oauthId); // OAuth id
        Post createdPost = postService.createPost(oauthId, postCreate);
        return ResponseEntity.ok(createdPost);
    }

    @GetMapping("/api/post")
    public ResponseEntity<List<Post>> GetPostList(
            Authentication authentication
    ) {
        String oauthId = authentication.getName();
        List<Post> postList = postService.getPostList();
        return ResponseEntity.ok(postList);
    }

    @GetMapping("/api/test")
    public void test(
            Authentication authentication
    ) {
        String userId = authentication.getName(); // 현재 인증된 사용자의 ID를 가져옴
        log.info("userId>> " + authentication.toString());
    }
}
