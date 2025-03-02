package com.longing.longing.comment.controller;

import com.longing.longing.comment.controller.port.CommentService;
import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.domain.CommentCreate;
import com.longing.longing.comment.domain.CommentResponse;
import com.longing.longing.comment.domain.CommentUpdate;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.post.infrastructure.PostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/post/{id}/comment")
    public ApiResponse<List<CommentResponse>> getCommentListByPost(
            @PathVariable("id") Long postId,
            @RequestParam(required = false, defaultValue = "0") Long lastCommentId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        List<Comment> commentList = commentService.getCommentList(postId, lastCommentId, limit);
        return ApiResponse.ok(commentList.stream()
                        .map(CommentResponse::fromDomain)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/comment")
    public ApiResponse<Comment> createComment(
            @RequestBody CommentCreate commentCreate,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        Comment comment = commentService.createComment(userDetails, commentCreate);
        return ApiResponse.created(comment);
    }

    @PatchMapping("/comment/{id}")
    public ApiResponse<Comment> updateComment(
            @PathVariable("id") Long commentId,
            @RequestBody CommentUpdate commentUpdate,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        Comment comment = commentService.updateComment(userDetails, commentId, commentUpdate);
        return ApiResponse.ok(comment);
    }

    @DeleteMapping("/comment/{id}")
    public ApiResponse<Void> deleteComment(
            @PathVariable("id") Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.deleteComment(userDetails, commentId);
        return ApiResponse.ok(null);
    }
}
