package com.longing.longing.comment.controller;

import com.longing.longing.comment.controller.port.CommentService;
import com.longing.longing.comment.domain.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/post/{id}/comment")
    public ResponseEntity<List<Comment>> getCommentListByPost(
            @PathVariable("id") Long postId,
            @RequestParam(required = false, defaultValue = "0") Long lastCommentId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        List<Comment> commentList = commentService.getCommentList(postId, lastCommentId, limit);
        return ResponseEntity.ok(commentList);
    }
}
