package com.longing.longing.like.service;


import com.longing.longing.like.controller.port.LikeService;
import com.longing.longing.like.domain.LikePost;
import com.longing.longing.like.service.port.PostLikeRepository;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.service.port.PostRepository;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public void likePost(LikePost likePost) {
        Post post = postRepository.findById(likePost.getPost().getId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        User user = userRepository.findById(likePost.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 이미 좋아요를 눌렀는지 확인
        if (postLikeRepository.findByPostAndUser(post, user).isPresent()) {
            throw new IllegalStateException("You already liked this post");
        }

        // 좋아요 생성 및 저장
        postLikeRepository.save(likePost);
    }

    @Override
    public void unlikePost(long likeId) {
        if (postLikeRepository.findById(likeId).isEmpty()) {
            throw new IllegalStateException("You did not like this post");
        }

        // 좋아요 삭제
        postLikeRepository.deleteById(likeId);
    }
}
