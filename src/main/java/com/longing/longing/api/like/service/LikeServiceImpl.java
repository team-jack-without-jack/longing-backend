package com.longing.longing.api.like.service;


import com.longing.longing.api.like.controller.port.LikeService;
import com.longing.longing.api.like.domain.LikePostDelete;
import com.longing.longing.api.like.domain.PostLike;
import com.longing.longing.api.like.service.port.PostLikeRepository;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.common.exceptions.AlreadyLikedException;
import com.longing.longing.api.like.domain.LikePostCreate;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

@Slf4j
@Service
@Builder
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {
    private final PostLikeRepository postLikeRepository;

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void likePost(LikePostCreate likePostCreate) {
        Post post = postRepository.findById(likePostCreate.getPostId(), likePostCreate.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        User user = userRepository.findById(likePostCreate.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 이미 좋아요를 눌렀는지 확인
        if (postLikeRepository.findByPostAndUser(post, user).isPresent()) {
            throw new AlreadyLikedException("You already liked this post");
        }

        postRepository.incrementLikeCount(post.getId());

        PostLike postLike = PostLike.builder()
                .post(post)
                .user(user)
                .build();
        postLikeRepository.save(postLike);
    }

    @Override
    @Transactional
    public void unlikePost(LikePostDelete likePostDelete) {
        Post post = postRepository.findById(likePostDelete.getPostId(), likePostDelete.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        User user = userRepository.findById(likePostDelete.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PostLike postLike = postLikeRepository.findByPostAndUser(post, user)
                .orElseThrow(() -> new EntityNotFoundException("User " + user.getEmail() + "did not like this post"));

//        post.unlike();
//        postRepository.save(post);

        postRepository.decrementLikeCount(post.getId());


        // 좋아요 삭제
        postLikeRepository.deleteById(postLike.getId());
    }

}
