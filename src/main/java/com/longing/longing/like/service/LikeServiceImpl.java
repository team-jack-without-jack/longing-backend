package com.longing.longing.like.service;


import com.longing.longing.comment.infrastructure.CommentJpaRepository;
import com.longing.longing.like.controller.port.LikeService;
import com.longing.longing.like.domain.LikePostCreate;
import com.longing.longing.like.domain.LikePostDelete;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.like.infrastructure.PostLikeJpaRepository;
import com.longing.longing.like.service.port.PostLikeRepository;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostLikeJpaRepository postLikeJpaRepository;
    private final CommentJpaRepository commentJpaRepository;

    private final PostJpaRepository postJpaRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void likePost(LikePostCreate likePostCreate) {
        PostEntity postEntity = postJpaRepository.findById(likePostCreate.getPostId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        User user = userRepository.findById(likePostCreate.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        // 이미 좋아요를 눌렀는지 확인
        if (postLikeRepository.findByPostAndUser(postEntity.toModel(), user).isPresent()) {
            throw new IllegalStateException("You already liked this post");
        }

        postEntity.like();

        // 좋아요 엔티티 생성 및 관계 설정
        PostLikeEntity postLikeEntity = PostLikeEntity.builder()
                .user(UserEntity.fromModel(user))
                .post(postEntity)
                .build();

        // 좋아요 생성 및 저장
        postLikeEntity.likePost(); // PostEntity와의 관계 설정
        postLikeRepository.save(postLikeEntity);

//        postLikeRepository.save(likePostCreate, postEntity.toModel(), user);
    }

    @Override
    @Transactional
    public void unlikePost(LikePostDelete likePostDelete) {
        PostEntity postEntity = postJpaRepository.findById(likePostDelete.getPostId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        User user = userRepository.findById(likePostDelete.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PostLikeEntity postLikeEntity = postLikeJpaRepository.findByPostIdAndUserId(postEntity.getId(), user.getId())
                .orElseThrow(() -> new IllegalStateException("You did not like this post"));

        postEntity.unlike();
        postLikeEntity.unlikePost();
        // 좋아요 삭제
        postLikeRepository.deleteById(postLikeEntity.getId());
    }

}
