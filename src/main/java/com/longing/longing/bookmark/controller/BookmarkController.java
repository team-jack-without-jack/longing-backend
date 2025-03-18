package com.longing.longing.bookmark.controller;

import com.longing.longing.bookmark.controller.port.BookmarkService;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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
}
