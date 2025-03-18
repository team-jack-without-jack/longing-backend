package com.longing.longing.bookmark.infrastructure;

import com.longing.longing.bookmark.service.port.BookmarkRepository;
import com.longing.longing.post.domain.Post;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepository {
    private final BookmarkJpaRepository bookmarkJpaRepository;

    public Optional<PostBookmark> findByPostAndUser(Post post, User user) {
        return bookmarkJpaRepository.findByPostIdAndUserId(post.getId(), user.getId()).map(PostBookmarkEntity::toModel);
    }

    @Override
    public void save(PostBookmarkEntity postBookmarkEntity) {
        bookmarkJpaRepository.save(postBookmarkEntity);
    }

    @Override
    public void deleteById(Long postId) {
        bookmarkJpaRepository.deleteById(postId);
    }
}
