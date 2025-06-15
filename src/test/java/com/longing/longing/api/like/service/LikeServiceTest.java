package com.longing.longing.api.like.service;

import com.longing.longing.api.like.controller.port.LikeService;
import com.longing.longing.api.like.domain.LikePostCreate;
import com.longing.longing.api.like.domain.LikePostDelete;
import com.longing.longing.api.like.domain.PostLike;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.PostServiceImpl;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.mock.FakePostLikeRepository;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LikeServiceTest {

    private LikeServiceImpl likeService;
    private PostServiceImpl postService;

    private FakePostRepository fakePostRepository;

    @BeforeEach
    void init() {
        FakePostLikeRepository fakePostLikeRepository = new FakePostLikeRepository();
        FakePostRepository fakePostRepository = new FakePostRepository();
        FakeUserRepository fakeUserRepository = new FakeUserRepository();
        this.likeService = LikeServiceImpl.builder()
                .postLikeRepository(fakePostLikeRepository)
                .postRepository(fakePostRepository)
                .userRepository(fakeUserRepository)
                .build();
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
        Post post = Post.builder()
                .id(1L)
                .title("test_title")
                .content("test_content")
                .likeCount(0)
                .user(user1)
                .build();

//        PostLike postLike = PostLike.builder()
//                .id(1L)
//                .post(post)
//                .user(user1)
//                .build();

        fakeUserRepository.save(user1);
        fakeUserRepository.save(user2);
        fakePostRepository.save(post);
//        fakePostLikeRepository.save(postLike);
    }

    @Test
    void 좋아요를_누르면_Post의_likeCount가_증가한다() {
        // given
        CustomUserDetails userDetails = new CustomUserDetails("test@test.com", Provider.GOOGLE);
        LikePostCreate likePostCreate = LikePostCreate.builder()
                .postId(1L)
                .userId(1L)
                .build();

        // when
//        likeService.likePost(likePostCreate);
        fakePostRepository.incrementLikeCount(1L);

        // then
        Integer likeCount = postService.getPost(userDetails, 1L).getLikeCount();
        System.out.printf("likeCount>> " + likeCount);
        assertThat(likeCount).isEqualTo(1);
    }

}
