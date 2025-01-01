package com.longing.longing.comment.controller;

import com.longing.longing.comment.controller.port.CommentService;
import com.longing.longing.comment.domain.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/post/{id}/comment")
    public ResponseEntity<Page<Comment>> getCommentByPost(
            @PathVariable("id") Long postId
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size
//            @RequestParam(defaultValue = "id") String sortBy,
//            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Page<Comment> commentList = commentService.getCommentList(postId);
        return ResponseEntity.ok(commentList);
    }
}
