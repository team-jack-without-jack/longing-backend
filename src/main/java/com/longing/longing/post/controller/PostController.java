package com.longing.longing.post.controller;

import com.longing.longing.post.controller.port.PostService;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            Authentication authentication,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        String oauthId= authentication.getName(); // 현재 인증된 사용자의 ID를 가져옴
        Post createdPost = postService.createPost(oauthId, postCreate, images);
        return ResponseEntity.ok(createdPost);
    }

    /**
     * 게시글 목록 가져오기
     * @return
     */
    @GetMapping("/api/post")
    public ResponseEntity<Page<Post>> GetPostList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Page<Post> postList = postService.getPostList(keyword, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(postList);
    }


    /**
     * 게시글 수정하기
     * @return
     */
    @PatchMapping("/api/post/{id}")
    public ResponseEntity<Post> updatePost(
//            @RequestParam long postId,
            @PathVariable("id") long postId,
            @RequestBody PostUpdate postUpdate,
            Authentication authentication
    ) {
        String oauthId = authentication.getName();
        Post post = postService.updatePost(oauthId, postId, postUpdate);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 삭제하기
     * @return
     */
    @DeleteMapping("/api/post/{id}")
    public void deletePost(
            @PathVariable("id") long postId
    ) {
        postService.deletePost(postId);
    }

    @GetMapping("/api/test")
    public void test(
            Authentication authentication
    ) {
        String userId = authentication.getName(); // 현재 인증된 사용자의 ID를 가져옴
    }
}
