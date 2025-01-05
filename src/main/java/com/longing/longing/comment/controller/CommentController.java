package com.longing.longing.comment.controller;

import com.longing.longing.comment.controller.port.CommentService;
import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.domain.CommentCreate;
import com.longing.longing.comment.domain.CommentResponse;
import com.longing.longing.comment.domain.CommentUpdate;
import com.longing.longing.post.infrastructure.PostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<List<CommentResponse>> getCommentListByPost(
            @PathVariable("id") Long postId,
            @RequestParam(required = false, defaultValue = "0") Long lastCommentId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        List<Comment> commentList = commentService.getCommentList(postId, lastCommentId, limit);
        return ResponseEntity.ok(commentList.stream()
                        .map(CommentResponse::fromDomain)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/comment")
    public ResponseEntity<Comment> createComment(
            Authentication authentication,
            @RequestBody CommentCreate commentCreate
            ) {
        String oauthId= authentication.getName();
        Comment comment = commentService.createComment(oauthId, commentCreate);
        return ResponseEntity.ok(comment);
    }

    @PatchMapping("/comment/{id}")
    public ResponseEntity<Comment> updateComment(
            Authentication authentication,
            @PathVariable("id") Long commentId,
            @RequestBody CommentUpdate commentUpdate
            ) {
        String oauthId = authentication.getName();
        Comment comment = commentService.updateComment(oauthId, commentId, commentUpdate);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/comment/{id}")
    public void deleteComment(
            @PathVariable("id") Long commentId,
            Authentication authentication
    ) {
        String oauthId = authentication.getName();
        commentService.deleteComment(oauthId, commentId);
    }
}
