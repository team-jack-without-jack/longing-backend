package com.longing.longing.api.bookmark.service;

import com.longing.longing.api.bookmark.controller.port.BookmarkService;
import com.longing.longing.api.bookmark.service.port.BookmarkRepository;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import com.longing.longing.api.bookmark.domain.PostBookmark;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.port.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Override
    @Transactional
    public void addBookmark(CustomUserDetails userDetails, Long postId) {
        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Post post = postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        // bookmark 여부 확인
        if (bookmarkRepository.findByPostAndUser(post, user).isPresent()) {
            throw new IllegalStateException("You already bookmarked this post");
        }

        PostBookmark postBookmark = PostBookmark.builder()
                .post(post)
                .user(user)
                .build();
        bookmarkRepository.save(postBookmark);
    }

    @Override
    @Transactional
    public void removeBookmark(CustomUserDetails userDetails, Long postId) {
        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Post post = postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        PostBookmark postBookmark = bookmarkRepository.findByPostAndUser(post, user)
                .orElseThrow(() -> new IllegalStateException("You did not bookmark this post"));

        bookmarkRepository.deleteById(postBookmark.getId());
    }

    @Override
    public Page<Post> getBookmarkPost(
            CustomUserDetails userDetails,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
            ) {
        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy)); // 첫 페이지, 10개씩 가져오기
        return bookmarkRepository.getBookmarkPost(user.getId(), pageable);
    }
}
