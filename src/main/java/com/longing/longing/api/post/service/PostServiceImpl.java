package com.longing.longing.api.post.service;

import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostCreate;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.common.ErrorCode;
import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.exceptions.CustomException;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.common.service.port.PostImageRepository;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.post.domain.PostUpdate;
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

import java.util.List;

@Slf4j
@Builder
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final S3ImageService s3ImageService;
    private final PostImageRepository postImageRepository;

    @Override
    @Transactional
    public Post createPost(User user, PostCreate postCreate, List<MultipartFile> images) {
        Post post = Post.from(user, postCreate);
        post = postRepository.save(post);

        log.info("title:: " + post.getTitle());
        log.info("content:: " + post.getContent());

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
        String imageUrl = s3ImageService.uploadPostImage(image, s3Dir);
        PostImage postImage = PostImage.from(imageUrl, post, user);
        postImageRepository.save(postImage);
    }

    private void checkPostIsMadeByUser(long postId, User user) {
        postRepository.findByIdAndUserId(postId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @Override
    public Page<Post> getPostList(User user, String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return postRepository.findAll(user.getId(), pageable);
        }
        return postRepository.findAllwithLikeCountAndSearch(user.getId(), keyword, pageable);
    }

    @Override
    public Page<Post> getMyPostList(User user, String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return postRepository.findMyPostsWithLikeCountAndSearch(user.getId(), keyword, pageable);
    }

    @Override
    public Post getPost(User user, Long postId) {
        return postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
    }

    /**
     * 게시글 수정 (#126).
     * 제목·본문·수정 시각만 UPDATE 한 문장으로 바꾼다. 읽어 둔 게시글 전체를 다시 저장(merge)하지 않는다.
     * merge는 그사이 바뀐 좋아요·댓글 수를 옛 값으로 덮어쓰고, 비어 있는 이미지 목록 때문에 기존 이미지를 모두 지웠다(재현 확인).
     * 수정 사이에 게시글이 삭제됐으면 바꾸지 않고 RESOURCE_NOT_FOUND다. 제목·본문은 마지막 요청의 값으로 쓰인다.
     * 응답은 다시 조회한 게시글이라 카운터와 이미지가 최신이다.
     * 게시글 조회(PostEntity.toModel)가 작성자와 이미지를 지연 로딩하므로 open-in-view가 켜져 있어야 한다(꺼지면 LazyInitializationException).
     */
    @Override
    public Post updatePost(User user, Long postId, PostUpdate postUpdate, List<MultipartFile> images) {
        Post post = postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot modify this post.");
        }

        for (MultipartFile image : images) {
            uploadAndSaveImage(image, post, user);
        }

        post.update(postUpdate);
        if (postRepository.updateContent(post.getId(), post.getTitle(), post.getContent()) == 0) {
            throw new ResourceNotFoundException("Posts", postId);
        }
        return postRepository.findById(postId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
    }

    @Override
    public void deletePost(Long postId, User user) {
        checkPostIsMadeByUser(postId, user);
        postRepository.deleteById(postId);
    }

    @Override
    public void blockPost(long postId) {
        postRepository.deleteById(postId);
    }

    @Override
    public Page<Post> getUserPostList(long userId, String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return postRepository.findByUserId(userId, pageable);
    }
}
