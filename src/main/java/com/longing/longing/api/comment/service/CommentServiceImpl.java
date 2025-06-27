package com.longing.longing.api.comment.service;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.api.comment.infrastructure.CommentEntity;
import com.longing.longing.api.comment.service.port.CommentRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.post.infrastructure.PostJpaRepository;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Builder
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostJpaRepository postJpaRepository;
    private final PostRepository postRepository;

    @Transactional
    public Comment _createComment(User user, CommentCreate commentCreate) {
        PostEntity postEntity = postJpaRepository.findById(commentCreate.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", commentCreate.getPostId()));

        Comment comment = Comment.from(user, postEntity.toModel(), commentCreate);

        CommentEntity commentEntity = CommentEntity.fromModel(comment);
        postEntity.addComment(commentEntity);
        return comment;
    }

    @Override
    @Transactional
    public Comment createComment(User user, CommentCreate commentCreate) {
        Post post = postRepository.findById(commentCreate.getPostId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", commentCreate.getPostId()));

        Comment comment = Comment.from(user, post, commentCreate);

        postRepository.incrementCommentCount(post.getId());
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(User user, long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comments", commentId));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("not a user who created");
        }

        Post post = postRepository.findById(comment.getPost().getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", comment.getPost().getId()));

        postRepository.decrementCommentCount(post.getId());
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional

    public Comment updateComment(User writer, long commentId, CommentUpdate commentUpdate) {
        Comment comment = commentRepository.findByIdAndUserId(commentId, writer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        log.info("comment" + comment.getId());
        log.info("comment" + comment.getContent());

        Comment updateComment = comment.update(commentUpdate);
        commentRepository.save(updateComment);
        return updateComment;
    }

    @Override
    public List<Comment> getCommentList(long postId, long lastCommentId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return commentRepository.getCommentList(postId, lastCommentId, pageable);
    }
}
