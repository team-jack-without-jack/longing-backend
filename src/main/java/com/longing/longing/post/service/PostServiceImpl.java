package com.longing.longing.post.service;

import com.longing.longing.post.controller.port.PostService;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

//    private final PostRepository postRepository;


    @Override
    public Post createPost(PostCreate postCreate) {
        return null;
    }

    @Override
    public List<Post> getPostList() {
        return null;
    }

    @Override
    public Post getPost(Long postId) {
        return null;
    }

    @Override
    public void updatePost(Long postId, PostUpdate postUpdate) {

    }

    @Override
    public void deletePost(Long postId) {

    }
}
