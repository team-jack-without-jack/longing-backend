package com.longing.longing.api.post.controller;

import com.longing.longing.api.comment.controller.response.CommentResponse;
import com.longing.longing.api.post.controller.response.PostResponse;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostCreate;
import com.longing.longing.api.post.domain.PostUpdate;
import com.longing.longing.api.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     * @param title
     * @param content
     * @param images
     * @param user
     * @return
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Post> CreatePost(
            @RequestPart("title") @NotBlank @Max(100) String title, // title 필드 받기
            @RequestPart("content") @NotBlank @Max(3000) String content, // content 필드 받기
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User user
    ) {
        log.info("=============");
        log.info("title> " + title);
        log.info("content> " + content);
        PostCreate postCreate = PostCreate.builder()
                .title(title)
                .content(content)
                .build();
        Post createdPost = postService.createPost(user, postCreate, images);
        return ApiResponse.created(createdPost);
    }

    /**
     * 게시글 목록 가져오기
     * @return
     */
    @GetMapping()
    public ApiResponse<Page<PostResponse>> GetPostList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(name = "myPost", defaultValue = "false") Boolean myPost,
            @AuthenticationPrincipal User user
    ) {
        if (myPost) {
            Page<Post> myPostList = postService.getMyPostList(user, keyword, page, size, sortBy, sortDirection);
            return ApiResponse.ok(myPostList.map(PostResponse::fromDomain));
        }
        Page<Post> postList = postService.getPostList(user, keyword, page, size, sortBy, sortDirection);
        Page<PostResponse> responsePage = postList.map(PostResponse::fromDomain);
        return ApiResponse.ok(responsePage);
    }


    /**
     * 게시글 수정하기
     * @return
     */
    @PatchMapping("/{id}")
    public ApiResponse<Post> updatePost(
            @PathVariable("id") long postId,
            PostUpdate postUpdate,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User user
    ) {
        List<MultipartFile> validatedImages = (images == null)
                ? Collections.emptyList()
                : images;

        Post post = postService.updatePost(user, postId, postUpdate, validatedImages);
        return ApiResponse.ok(post);
    }

    /**
     * 게시글 삭제하기
     * @return
     */
    @DeleteMapping("/{id}")
    public ApiResponse<?> deletePost(
            @PathVariable("id") long postId,
            @AuthenticationPrincipal User user
    ) {
        postService.deletePost(postId, user);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Post> getPost(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal User user
    ) {
        Post post = postService.getPost(user, postId);
        return ApiResponse.ok(post);
    }

    @GetMapping("/my")
    public ApiResponse<Page<Post>> getMyPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal User user
    ) {
        Page<Post> postList = postService.getMyPostList(user, keyword, page, size, sortBy, sortDirection);
        return ApiResponse.ok(postList);
    }

    @PostMapping("/block")
    public ApiResponse<?> blockPost(
            @RequestParam(defaultValue = "0") long postId,
            @AuthenticationPrincipal User user
    ) {
        postService.blockPost(postId);
        return ApiResponse.ok(null);
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
