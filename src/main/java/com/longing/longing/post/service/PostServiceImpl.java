package com.longing.longing.post.service;

import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.infrastructure.PostImageEntity;
import com.longing.longing.common.infrastructure.PostImageJpaRepository;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.common.service.port.PostImageRepository;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.post.controller.port.PostService;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.post.service.port.PostRepository;
import com.longing.longing.user.Provider;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
import com.longing.longing.user.service.port.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import java.util.List;

@Slf4j
@Builder
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final S3ImageService s3ImageService;
    private final PostImageRepository postImageRepository;


    @Override
    @Transactional
    public Post createPost(CustomUserDetails userDetails, PostCreate postCreate, List<MultipartFile> images) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        // PostEntity 저장 (영속 상태로 만듦)
        Post post = Post.from(user, postCreate);
        post = postRepository.save(post);

        // 이미지 업로드 및 저장
        if (images != null && !images.isEmpty()) {
            log.info("image is exists!");
            for (MultipartFile image : images) {
                uploadAndSaveImage(image, post, user);
            }
        }

        return post;
    }

    private void uploadAndSaveImage(MultipartFile image, Post post, User user) {
        String s3Dir = "post_images/post_" + post.getId() + "/";
        String imageUrl = s3ImageService.upload(image, s3Dir);
        PostImage postImage = PostImage.from(imageUrl, post, user);
        postImageRepository.save(postImage);
    }

    @Override
    public Page<Post> getPostList(CustomUserDetails userDetails, String keyword, int page, int size, String sortBy, String sortDirection) {
        User user = userRepository.findByEmailAndProvider(userDetails.getEmail(), userDetails.getProvider())
                .orElseThrow(() -> new ResourceNotFoundException("Users", userDetails.getEmail()));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return postRepository.findAll(user.getId(), pageable);
        }
        return postRepository.findAllwithLikeCountAndSearch(user.getId(), keyword, pageable);
    }

    @Override
    public Page<Post> getMyPostList(CustomUserDetails userDetails, String keyword, int page, int size, String sortBy, String sortDirection) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return postRepository.findMyPostsWithLikeCountAndSearch(user.getId(), keyword, pageable);

    }

    @Override
    public Post getPost(CustomUserDetails userDetails, Long postId) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));


        return postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
    }

    @Override
    public Post updatePost(CustomUserDetails userDetails, Long postId, PostUpdate postUpdate, List<MultipartFile> images) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
        Post post = postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot modify this post.");
        }

        for (MultipartFile image : images) {
            uploadAndSaveImage(image, post, user);
        }

        post.update(postUpdate);
        return postRepository.save(post);
    }

    @Override
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }

}
