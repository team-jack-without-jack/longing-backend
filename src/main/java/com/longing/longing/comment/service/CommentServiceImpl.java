package com.longing.longing.comment.service;

import com.longing.longing.comment.controller.port.CommentService;
import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.domain.CommentCreate;
import com.longing.longing.comment.domain.CommentUpdate;
import com.longing.longing.comment.service.port.CommentRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.service.port.PostRepository;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Override
    public Comment createComment(String oauthId, CommentCreate commentCreate) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));
        Post post = postRepository.findById(commentCreate.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", commentCreate.getPostId()));
        Comment comment = Comment.from(user, post, commentCreate);
        return commentRepository.save(comment);
    }

    @Override
    public void deleteComment(long commentId) {

    }

//    @Override
//    @Transactional
//    public List<Comment> getCommentsByPostId(Long postId) {
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new EntityNotFoundException("Post not found with id " + postId));
//
//        return post.getCommentList();
//    }

    @Override
    public Comment updateComment(long commentId, CommentUpdate commentUpdate) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        comment.update(commentUpdate);
        return commentRepository.save(comment);
    }

    @Override
    public List<Comment> getCommentList(long postId, long lastCommentId, int limit) {
        return commentRepository.getCommentList(postId, lastCommentId, limit);
    }

}
