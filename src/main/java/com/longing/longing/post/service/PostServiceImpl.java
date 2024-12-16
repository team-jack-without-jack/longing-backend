package com.longing.longing.post.service;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.post.controller.port.PostService;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.post.service.port.PostRepository;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;


    @Override
    public Post createPost(String oauthId, PostCreate postCreate) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));;
        Post post = Post.from(user, postCreate);
        return postRepository.save(post);
    }

    @Override
    public List<Post> getPostList() {
        return postRepository.findAll();
    }

    @Override
    public Post getPost(Long postId) {
        return null;
    }

    @Override
    public Post updatePost(Long postId, PostUpdate postUpdate) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
        post.update(postUpdate);
        return postRepository.save(post);
    }

    @Override
    public void deletePost(Long postId) {

    }
}
