package com.longing.longing.api.user.service;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.PostServiceImpl;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.domain.PostImage;
import com.longing.longing.mock.FakePostImageRepository;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class UserServiceTest {

    private UserServiceImpl userService;

    @BeforeEach
    void init() {
        FakeUserRepository fakeUserRepository = new FakeUserRepository();
        FakePostImageRepository fakePostImageRepository = new FakePostImageRepository();
        this.userService = UserServiceImpl.builder()
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
        Post post = Post.builder()
                .id(1L)
                .title("test_title")
                .content("test_content")
                .user(user1)
                .build();

        fakeUserRepository.save(user1);
        fakeUserRepository.save(user2);
//        fakePostRepository.save(post);

    }

//    @Test
//    void 유저를_블락할_수_있다() {
//        // given
//        User user3 = User.builder()
//                .id(3L)
//                .email("test3@test.com")
//                .name("test_name3")
//                .nationality("KOREA")
//                .introduction("hello world")
//                .role(Role.GUEST)
//                .provider(Provider.GOOGLE)
//                .providerId("3")
//                .picture("test_picture")
//                .build();
//
//        long blockedUserId = 1L;
//
//        // when
//        userService.blockUser(user3, blockedUserId);
//
//        // then
//
//    }

    @Test
    void 유저프로필을_조회할_수_있다() {
        // given

        // when
        User user = userService.getUserProfile(1L);

        // then
        assertThat(user.getName()).isEqualTo("test_name");
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getRole()).isEqualTo(Role.GUEST);
        assertThat(user.getIntroduction()).isEqualTo("hello world");
        assertThat(user.getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(user.getProviderId()).isEqualTo("1");
        assertThat(user.getPicture()).isEqualTo("test_picture");
    }

}
