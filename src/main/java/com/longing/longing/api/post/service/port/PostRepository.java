package com.longing.longing.api.post.service.port;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostRepository {
    Optional<Post> findById(Long id, Long userId);
    Optional<Post> findByIdForUpdate(Long id);

    Post save(Post post);

    /**
     * 제목·본문·수정 시각만 바꾸고 바뀐 행 수를 반환한다. 없거나 삭제된 게시글이면 0이다.
     * 좋아요·댓글 수와 이미지는 그대로 둔다. 요청 EntityManager를 반영 후 비운다 (#126).
     */
    int updateContent(long postId, String title, String content);

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

    Page<Post> findByUserId(long userId, Pageable pageable);
}
