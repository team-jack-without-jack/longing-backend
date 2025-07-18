package com.longing.longing.api.post.service;

import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.mock.FakePostImageRepository;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostUpdate;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PostServiceTest {

    private PostServiceImpl postService;

    @BeforeEach
    void init() {
        FakePostRepository fakePostRepository = new FakePostRepository();
        FakeUserRepository fakeUserRepository = new FakeUserRepository();
        FakePostImageRepository fakePostImageRepository = new FakePostImageRepository();
        this.postService = PostServiceImpl.builder()
                .postRepository(fakePostRepository)
                .postImageRepository(fakePostImageRepository)
                .build();
        User user1 = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .nationality("KOREA")
                .introduction("hello world")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .picture("test_picture")
                .build();
        User user2 = User.builder()
                .id(2L)
                .email("test2@test.com")
                .name("test_name2")
                .nationality("JAPAN")
                .introduction("hello world2")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("2")
                .picture("test_picture2")
                .build();
        Post post = Post.builder()
                .id(1L)
                .title("test_title")
                .content("test_content")
                .user(user1)
                .build();

        fakeUserRepository.save(user1);
        fakeUserRepository.save(user2);
        fakePostRepository.save(post);

        MockMultipartFile multipartFile1 = new MockMultipartFile("file", "test.txt", "text/plain", "test file".getBytes(StandardCharsets.UTF_8) );
        MockMultipartFile multipartFile2 = new MockMultipartFile("file", "test2.txt", "text/plain", "test file2".getBytes(StandardCharsets.UTF_8) );

        PostImage postImage = PostImage.builder()
                .id(1L)
                .address("image_address")
                .post(post)
                .user(user1)
                .build();

        fakePostImageRepository.save(postImage);
    }

    @Test
    void getPost는_존재하는_게시물을_내려준다() {
        // given
//        CustomUserDetails userDetails = new CustomUserDetails("test@test.com", Provider.GOOGLE);
        User user1 = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .nationality("KOREA")
                .introduction("hello world")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .picture("test_picture")
                .build();
        // when
        Post result = postService.getPost(user1, 1L);

        // then
        assertThat(result.getTitle()).isEqualTo("test_title");
        assertThat(result.getContent()).isEqualTo("test_content");
    }

    @Test
    void updatePost는_존재하는_게시글을_수정한다() {
        // given
//        CustomUserDetails userDetails = new CustomUserDetails("test@test.com", Provider.GOOGLE);
        User user1 = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .nationality("KOREA")
                .introduction("hello world")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .picture("test_picture")
                .build();
        Long postId = 1L;
        PostUpdate postUpdate = PostUpdate.builder()
                .title("update_title")
                .content("update_content")
                .build();
        List<MultipartFile> images = Collections.emptyList();

        // when
        Post result = postService.updatePost(user1, 1L, postUpdate, images);

        // then
        assertThat(result.getTitle()).isEqualTo(postUpdate.getTitle());
        assertThat(result.getContent()).isEqualTo(postUpdate.getContent());
    }

    @Test
    void 없는_id_를_찾으면_에러를_발생시킨다() {
        //given
        User user1 = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .nationality("KOREA")
                .introduction("hello world")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .picture("test_picture")
                .build();

        Long postId = 5L;

        // when

        // then
        assertThatThrownBy(() -> {
            postService.getPost(user1, postId);
        }).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 게시글을_삭제한다() {
        // given
        User user1 = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .nationality("KOREA")
                .introduction("hello world")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .picture("test_picture")
                .build();

        Long postId = 1L;

        // when
        postService.deletePost(postId, user1);

        // then
        assertThatThrownBy(() -> {
            postService.getPost(user1, postId);
        }).isInstanceOf(ResourceNotFoundException.class);

    }
}
