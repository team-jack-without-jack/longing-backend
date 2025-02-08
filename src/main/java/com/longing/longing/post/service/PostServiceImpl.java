package com.longing.longing.post.service;

import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.infrastructure.PostImageEntity;
import com.longing.longing.common.infrastructure.PostImageJpaRepository;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.common.service.port.PostImageRepository;
import com.longing.longing.post.controller.port.PostService;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.post.infrastructure.PostJpaRepository;
import com.longing.longing.post.service.port.PostRepository;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.infrastructure.UserEntity;
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
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    @PersistenceContext
    private EntityManager entityManager;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostJpaRepository postJpaRepository;
    private final S3ImageService s3ImageService;
    private final PostImageRepository postImageRepository;
    private final PostImageJpaRepository postImageJpaRepository;





    private boolean isPersistent(PostEntity postEntity) {
        return entityManager.contains(postEntity);
    }

    @Override
    @Transactional
    public Post createPost(String oauthId, PostCreate postCreate, List<MultipartFile> images) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));

        // PostEntity 저장 (영속 상태로 만듦)
        Post post = Post.from(user, postCreate);
        PostEntity postEntity = PostEntity.fromModel(post);
        UserEntity userEntity = UserEntity.fromModel(user);
        postEntity = postJpaRepository.save(postEntity); // 🔥 여기서 먼저 저장

        // 이미지 업로드 및 저장
        for (MultipartFile image : images) {
            log.info("Uploading image: " + image.getOriginalFilename());
            uploadAndSaveImage(image, postEntity, userEntity);
        }

        return post;
    }

    private void uploadAndSaveImage(MultipartFile image, PostEntity postEntity, UserEntity userEntity) {
//        // S3에 이미지 업로드
//        String imageUrl = s3ImageService.upload(image);
//
//        // PostEntity가 영속 상태이므로 바로 저장 가능
//        PostImageEntity postImage = new PostImageEntity(imageUrl, postEntity, userEntity);
//        postEntity.addImage(postImage, userEntity); // 🔥 Post에 이미지 추가
//        postJpaRepository.save(postEntity); // 🔥 다시 저장하여 이미지도 반영
        // S3에 이미지 업로드
        String imageUrl = s3ImageService.upload(image);

        // PostEntity가 영속 상태이므로 바로 저장 가능
        PostImageEntity postImage = new PostImageEntity(imageUrl, postEntity, userEntity);
        postEntity.addImage(postImage, userEntity); // Post에 이미지 추가
        // 영속 상태에서 추가된 이미지는 따로 save()할 필요 없이 자동 반영됨
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

    @Override
    public Page<Post> getMyPostList(String oauthId, String keyword, int page, int size, String sortBy, String sortDirection) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
//        return postRepository.findAll(pageable);

        if (keyword == null || keyword.trim().isEmpty()) {
            return postRepository.findAll(pageable);
        }
        return postRepository.findMyPostsWithLikeCountAndSearch(user.getId(), keyword, pageable);

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
    public Post getPost(String oauthId, Long postId) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));

        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
    }

    @Override
    @Transactional
    public Post updatePost(String oauthId, Long postId, PostUpdate postUpdate, List<MultipartFile> images) {
//        PostEntity postEntity = postJpaRepository.findById(postId)
//                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
//        User user = userRepository.findByProviderId(oauthId)
//                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));
//        if (!postEntity.getUser().getId().equals(user.getId())) {
//            throw new AccessDeniedException("you can not modify this location");
//        }
//        Post post = postEntity.toModel();
//        Post updatedPost = post.update(postUpdate);
//        postEntity.update(postUpdate);
//        return updatedPost;

        // 1. 기존 포스트 엔티티 조회
        PostEntity postEntity = postJpaRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));

        // 2. 유저 조회 및 권한 체크
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));

        if (!postEntity.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot modify this post.");
        }

        // 3. 포스트 정보 업데이트
//        Post post = postEntity.toModel();
//        Post updatedPost = post.update(postUpdate);  // update() 메서드는 제목, 내용 등의 정보를 업데이트
//        PostEntity updatedPost = postEntity.update(postUpdate);
        UserEntity userEntity = UserEntity.fromModel(user);

        // 4. 기존 이미지 삭제 (리스트를 비워서 JPA가 삭제하도록 유도)
        postEntity.getPostImageEntities().clear();  // 이미지를 모두 제거 (orphanRemoval이 적용되어 있으면 DB에서 삭제됨)


        // 5. 새 이미지 업로드 및 저장
        for (MultipartFile image : images) {
            uploadAndSaveImage(image, postEntity, userEntity);  // uploadAndSaveImage() 메서드로 새 이미지를 처리
        }

        // 6. PostEntity 업데이트 (이미지 포함)
        postEntity.update(postUpdate);

        // 7. 변경된 Post 반환
        return postEntity.toModel();
    }


    @Override
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }
}
