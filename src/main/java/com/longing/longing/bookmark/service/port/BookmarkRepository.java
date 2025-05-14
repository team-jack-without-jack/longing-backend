package com.longing.longing.bookmark.service.port;

import com.longing.longing.bookmark.infrastructure.PostBookmarkEntity;
import com.longing.longing.post.domain.Post;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository {
    Optional<PostBookmark> findByPostAndUser(Post post, User user);
//    void save(PostBookmarkEntity postBookmarkEntity);
    void save(PostBookmark postBookmark);

    void deleteById(Long postId);

    Page<Post> getBookmarkPost(Long userId, Pageable pageable);

//    Optional<PostBookmark> findByPostIdAndUserId(long postId, long userId);
}
