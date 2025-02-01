package com.longing.longing.post.service.port;

import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Optional<Post> findById(Long id);

    Post save(Post post);

    Page<Post> findAll(Pageable pageable);

    void deleteById(Long postId);

    Page<Post> findAllwithLikeCountAndSearch(String keyword, Pageable pageable);
    void flush();
}
