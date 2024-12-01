package com.longing.longing.post.controller.port;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;

import java.util.List;

public interface PostService {
    Post createPost(PostCreate postCreate);
    List<Post> getPostList();
    Post getPost(Long postId);
    void updatePost(Long postId, PostUpdate postUpdate);
    void deletePost(Long postId);
}
