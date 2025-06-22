package com.longing.longing.api.comment.controller;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.controller.response.CommentResponse;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
            @RequestBody @Valid CommentCreate commentCreate,
            @AuthenticationPrincipal User user
            ) {
        Comment comment = commentService.createComment(user, commentCreate);
        return ApiResponse.created(comment);
    }

    @PatchMapping("/comment/{id}")
    public ApiResponse<Comment> updateComment(
            @PathVariable("id") Long commentId,
            @RequestBody CommentUpdate commentUpdate,
            @AuthenticationPrincipal User user
            ) {
        Comment comment = commentService.updateComment(user, commentId, commentUpdate);
        return ApiResponse.ok(comment);
    }

    @DeleteMapping("/comment/{id}")
    public ApiResponse<Void> deleteComment(
            @PathVariable("id") Long commentId,
            @AuthenticationPrincipal User user
    ) {
        commentService.deleteComment(user, commentId);
        return ApiResponse.ok(null);
    }
}
