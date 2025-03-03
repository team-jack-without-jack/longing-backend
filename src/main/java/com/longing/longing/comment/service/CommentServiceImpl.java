package com.longing.longing.comment.service;

import com.longing.longing.comment.controller.port.CommentService;
import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.domain.CommentCreate;
import com.longing.longing.comment.domain.CommentUpdate;
import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.comment.infrastructure.CommentJpaRepository;
import com.longing.longing.comment.service.port.CommentRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.post.service.port.PostRepository;
import com.longing.longing.user.Provider;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentJpaRepository commentJpaRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostJpaRepository postJpaRepository;

    @Override
    @Transactional
    public Comment createComment(CustomUserDetails userDetails, CommentCreate commentCreate) {
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
    public void deleteComment(CustomUserDetails userDetails, long commentId) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
        CommentEntity commentEntity = commentJpaRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comments", commentId));

        if (!commentEntity.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("not a user who created");
        }

        PostEntity postEntity = commentEntity.getPost();
        postEntity.removeComment(commentEntity);
        commentRepository.deleteById(commentId);
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
    @Transactional
    public Comment updateComment(CustomUserDetails userDetails, long commentId, CommentUpdate commentUpdate) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User writer = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        CommentEntity commentEntity = commentJpaRepository.findByIdAndUserId(commentId, writer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        Comment comment = commentEntity.toModel();
        Comment updatedComment = comment.update(commentUpdate);
        commentEntity.update(commentUpdate);
        return updatedComment;
    }

    @Override
    public List<Comment> getCommentList(long postId, long lastCommentId, int limit) {
        // Pageable 객체 생성 (페이지 0, 지정된 limit)
        Pageable pageable = PageRequest.of(0, limit);
        return commentRepository.getCommentList(postId, lastCommentId, pageable);
        /**
         * TODO or not?
         * Post domain commentListAdd
         */
    }

}
