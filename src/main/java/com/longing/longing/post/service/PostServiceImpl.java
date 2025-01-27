package com.longing.longing.post.service;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.post.controller.port.PostService;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.post.service.port.PostRepository;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostJpaRepository postJpaRepository;


    @Override
    public Post createPost(String oauthId, PostCreate postCreate) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));
        Post post = Post.from(user, postCreate);
        return postRepository.save(post);
    }

    @Override
    public Page<Post> getPostList(String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
//        return postRepository.findAll(pageable);

        if (keyword == null || keyword.trim().isEmpty()) {
            return postRepository.findAll(pageable);
        }
        return postRepository.findAllwithLikeCountAndSearch(keyword, pageable);
    }

//    @Override
//    public List<Post> getPostList(String keyword) {
//        if (keyword == null || keyword.trim().isEmpty()) {
//            // 키워드가 없으면 모든 게시글 조회
//            return postRepository.findAllWithLikeCount();
//        }
//        // 키워드가 있으면 검색된 게시글 조회
//        return postRepository.findAllWithLikeCountByKeyword(keyword);
//    }

    @Override
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
    }

    @Override
    @Transactional
    public Post updatePost(String oauthId, Long postId, PostUpdate postUpdate) {
        PostEntity postEntity = postJpaRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));
        if (!postEntity.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("you can not modify this location");
        }
        Post post = postEntity.toModel();
        Post updatedPost = post.update(postUpdate);
        postEntity.update(postUpdate);
        return updatedPost;
    }

    @Override
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }
}
