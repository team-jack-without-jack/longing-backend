package com.longing.longing.bookmark.service;

import com.longing.longing.bookmark.controller.port.BookmarkService;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.bookmark.infrastructure.BookmarkJpaRepository;
import com.longing.longing.bookmark.infrastructure.PostBookmarkEntity;
import com.longing.longing.bookmark.service.port.BookmarkRepository;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkJpaRepository bookmarkJpaRepository;
    private final UserRepository userRepository;
    private final PostJpaRepository postJpaRepository;

    @Override
    public void addBookmark(CustomUserDetails userDetails, Long postId) {
        PostEntity postEntity = postJpaRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // bookmark 여부 확인
        if (bookmarkRepository.findByPostAndUser(postEntity.toModel(), user).isPresent()) {
            throw new IllegalStateException("You already bookmarked this post");
        }

        PostBookmarkEntity postBookmarkEntity = PostBookmarkEntity.builder()
                .user(UserEntity.fromModel(user))
                .post(postEntity)
                .build();

//        postLikeEntity.likePost(); // PostEntity와의 관계 설정
//        postLikeRepository.save(postLikeEntity);

        postBookmarkEntity.addBookmark();
        bookmarkRepository.save(postBookmarkEntity);
    }

    @Override
    public void removeBookmark(CustomUserDetails userDetails, Long postId) {
        PostEntity postEntity = postJpaRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PostBookmarkEntity postBookmarkEntity = bookmarkJpaRepository.findByPostIdAndUserId(postEntity.getId(), user.getId())
                .orElseThrow(() -> new IllegalStateException("You did not bookmark this post"));

        postBookmarkEntity.removeBookmark();
        bookmarkRepository.deleteById(postBookmarkEntity.getId());
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
