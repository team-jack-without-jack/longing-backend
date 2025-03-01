package com.longing.longing.post.controller.port;

import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {
    Post createPost(CustomUserDetails userDtails, PostCreate postCreate, List<MultipartFile> images);

    Page<Post> getPostList(String keyword, int page, int size, String sortBy, String sortDirection);
    Page<Post> getMyPostList(CustomUserDetails userDetails, String keyword, int page, int size, String sortBy, String sortDirection);
    Post getPost(Long postId);
    Post updatePost(CustomUserDetails userDetails, Long postId, PostUpdate postUpdate, List<MultipartFile> images);
    void deletePost(Long postId);
}
