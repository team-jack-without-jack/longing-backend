package com.longing.longing.api.post.service.port;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostRepository {
    Optional<Post> findById(Long id, Long userId);

    Post save(Post post);

    Page<Post> findAll(Long userId, Pageable pageable);

    void deleteById(Long postId);

    Page<Post> findAllwithLikeCountAndSearch(Long userId, String keyword, Pageable pageable);
    Page<Post> findMyPostsWithLikeCountAndSearch(Long userId, String keyword, Pageable pageable);

    Optional<Post> findByIdAndUserId(Long id, Long userId);

    void incrementLikeCount(Long postId);
    void decrementLikeCount(Long postId);
  
    void incrementCommentCount(Long postId);
    void decrementCommentCount(Long postId);

    void flush();
}
