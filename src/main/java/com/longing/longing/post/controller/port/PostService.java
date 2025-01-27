package com.longing.longing.post.controller.port;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostService {
    Post createPost(String oauthId, PostCreate postCreate);

    Page<Post> getPostList(String keyword, int page, int size, String sortBy, String sortDirection);
    Post getPost(String oauthId, Long postId);
    Post updatePost(String oauthId, Long postId, PostUpdate postUpdate);
    void deletePost(Long postId);
}
