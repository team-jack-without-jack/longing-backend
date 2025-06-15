package com.longing.longing.post.service;

import com.longing.longing.api.post.service.PostServiceImpl;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostUpdate;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PostServiceTest {

    private PostServiceImpl postService;

    @BeforeEach
    void init() {
        FakePostRepository fakePostRepository = new FakePostRepository();
        FakeUserRepository fakeUserRepository = new FakeUserRepository();
        this.postService = PostServiceImpl.builder()
                .postRepository(fakePostRepository)
                .userRepository(fakeUserRepository)
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

        fakeUserRepository.save(user1);
        fakeUserRepository.save(user2);
        fakePostRepository.save(Post.builder()
                .id(1L)
                .title("test_title")
                .content("test_content")
                .user(user1)
                .build());
    }

    @Test
    void getPost는_존재하는_게시물을_내려준다() {
        // given
        CustomUserDetails userDetails = new CustomUserDetails("test@test.com", Provider.GOOGLE);
        // when
        Post result = postService.getPost(userDetails, 1L);

        // then
        assertThat(result.getTitle()).isEqualTo("test_title");
        assertThat(result.getContent()).isEqualTo("test_content");
    }

    @Test
    void updatePost는_존재하는_게시글을_수정한다() {
        // given
        CustomUserDetails userDetails = new CustomUserDetails("test@test.com", Provider.GOOGLE);
        Long postId = 1L;
        PostUpdate postUpdate = PostUpdate.builder()
                .title("update_title")
                .content("update_content")
                .build();
        List<MultipartFile> images = Collections.emptyList();

        // when
        Post result = postService.updatePost(userDetails, 1L, postUpdate, images);

        // then
        assertThat(result.getTitle()).isEqualTo(postUpdate.getTitle());
        assertThat(result.getContent()).isEqualTo(postUpdate.getContent());
    }
}
