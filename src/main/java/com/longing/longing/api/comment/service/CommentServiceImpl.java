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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostJpaRepository postJpaRepository;
    private final PostRepository postRepository;

    @Transactional
    public Comment _createComment(CustomUserDetails userDetails, CommentCreate commentCreate) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        PostEntity postEntity = postJpaRepository.findById(commentCreate.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", commentCreate.getPostId()));

        Comment comment = Comment.from(user, postEntity.toModel(), commentCreate);

        CommentEntity commentEntity = CommentEntity.fromModel(comment);
        postEntity.addComment(commentEntity);
        return comment;
    }

    @Override
    @Transactional
    public Comment createComment(CustomUserDetails userDetails, CommentCreate commentCreate) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();

        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
        Post post = postRepository.findById(commentCreate.getPostId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", commentCreate.getPostId()));

        Comment comment = Comment.from(user, post, commentCreate);

//        post.addCommentCount();
//        postRepository.save(post);

        postRepository.incrementCommentCount(post.getId());
        commentRepository.save(comment);
        return comment;
    }

    @Override
    @Transactional
    public void deleteComment(CustomUserDetails userDetails, long commentId) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comments", commentId));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("not a user who created");
        }

        Post post = postRepository.findById(comment.getPost().getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", comment.getPost().getId()));

//        post.removeCommentCount();
//        postRepository.save(post);
        postRepository.decrementCommentCount(post.getId());
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public Comment updateComment(CustomUserDetails userDetails, long commentId, CommentUpdate commentUpdate) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User writer = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        Comment comment = commentRepository.findByIdAndUserId(commentId, writer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        commentRepository.save(comment);
        return comment.update(commentUpdate);
    }

    @Override
    public List<Comment> getCommentList(long postId, long lastCommentId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return commentRepository.getCommentList(postId, lastCommentId, pageable);
    }
}
