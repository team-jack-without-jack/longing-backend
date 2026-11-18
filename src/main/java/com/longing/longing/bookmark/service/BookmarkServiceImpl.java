package com.longing.longing.bookmark.service;

import com.longing.longing.bookmark.controller.port.BookmarkService;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.bookmark.service.port.BookmarkRepository;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    /**
     * 멱등: 이미 북마크했으면 그대로 성공한다 (#124).
     * 동시 요청이 모두 "없음"을 봐도 saveIfAbsent가 unique 제약으로 한 행만 남긴다.
     * saveIfAbsent는 트랜잭션 밖에서 호출해야 하므로 @Transactional을 붙이지 않는다.
     * 게시글 조회(PostEntity.toModel)가 작성자를 지연 로딩하므로 open-in-view가 켜져 있어야 한다(꺼지면 LazyInitializationException).
     * 게시글이 없거나 지워졌으면 RESOURCE_NOT_FOUND다. 새로 저장할 때는 saveIfAbsent가 게시글 행을 잠근 뒤 다시 확인하므로,
     * 읽은 뒤 저장 전에 지워진 경우도 같다 (#133).
     */
    @Override
    public void addBookmark(CustomUserDetails userDetails, Long postId) {
        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Post post = postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));

        if (bookmarkRepository.exists(user.getId(), post.getId())) {
            return;
        }
        bookmarkRepository.saveIfAbsent(PostBookmark.builder()
                .post(post)
                .user(user)
                .build());
    }

    /**
     * 멱등: 북마크가 없어도 성공하고, 중복 행이 남아 있으면 함께 지운다 (#124).
     * 게시글 조회(PostEntity.toModel)가 작성자를 지연 로딩하므로 open-in-view가 켜져 있어야 한다(꺼지면 LazyInitializationException).
     */
    @Override
    public void removeBookmark(CustomUserDetails userDetails, Long postId) {
        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Post post = postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        bookmarkRepository.delete(user.getId(), post.getId());
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
