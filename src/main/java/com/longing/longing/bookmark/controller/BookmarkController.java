package com.longing.longing.bookmark.controller;

import com.longing.longing.bookmark.controller.port.BookmarkService;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/bookmark")
@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * bookmark 추가
     * @param postId
     * @param userDetails
     * @return
     */
    @PostMapping("/{id}")
    public ApiResponse<Boolean> addBookmark(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        bookmarkService.addBookmark(userDetails, postId);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> removeBookmark(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        bookmarkService.removeBookmark(userDetails, postId);
        return ApiResponse.ok(true);
    }

    @GetMapping()
    public ApiResponse<Page<Post>> getBookmarkPost(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Page<Post> bookmarkPost = bookmarkService.getBookmarkPost(
                userDetails,
                page,
                size,
                sortBy,
                sortDirection);
        return ApiResponse.ok(bookmarkPost);
    }

}
