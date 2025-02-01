package com.longing.longing.post.controller;

import com.longing.longing.common.response.ApiResponse;
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
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     * @param postCreate
     * @param authentication
     * @return
     */
    @PostMapping()
    public ApiResponse<Post> CreatePost(
            PostCreate postCreate,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) {
        log.info("호출!!!!");
        String oauthId= authentication.getName(); // 현재 인증된 사용자의 ID를 가져옴
        Post createdPost = postService.createPost(oauthId, postCreate, images);
        return ApiResponse.created(createdPost);
    }

    /**
     * 게시글 목록 가져오기
     * @return
     */
    @GetMapping()
    public ApiResponse<Page<Post>> GetPostList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Page<Post> postList = postService.getPostList(keyword, page, size, sortBy, sortDirection);
        return ApiResponse.ok(postList);
    }


    /**
     * 게시글 수정하기
     * @return
     */
    @PatchMapping("/{id}")
    public ApiResponse<Post> updatePost(
//            @RequestParam long postId,
            @PathVariable("id") long postId,
            PostUpdate postUpdate,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) {
        String oauthId = authentication.getName();
        Post post = postService.updatePost(oauthId, postId, postUpdate, images);
        return ApiResponse.ok(post);
    }

    /**
     * 게시글 삭제하기
     * @return
     */
    @DeleteMapping("/{id}")
    public ApiResponse<?> deletePost(
            @PathVariable("id") long postId
    ) {
        postService.deletePost(postId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Post> getPost(
            @PathVariable("id") Long postId,
            Authentication authentication
    ) {
        String oauthId = authentication.getName();
        Post post = postService.getPost(oauthId, postId);
        return ApiResponse.ok(post);
    }

    @GetMapping("/my")
    public void getMyPosts(
            Authentication authentication
    ) {
//        postService.getPostList();
    }

    // 응답 데이터를 담을 내부 정적 클래스
    static class DeleteResponse {
        private final boolean result;

        public DeleteResponse(boolean result) {
            this.result = result;
        }

        public boolean isResult() {
            return result;
        }
    }
}
