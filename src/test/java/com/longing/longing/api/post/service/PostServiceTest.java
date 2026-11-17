package com.longing.longing.api.post.service;

import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.mock.FakePostImageRepository;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostCreate;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Test
    void createPostStoresClientRequestIdAndFindsItOnlyForTheSameUser() {
        // given
        User writer = User.builder()
                .id(7L)
                .email("writer@test.com")
                .name("writer")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("7")
                .build();
        PostCreate postCreate = PostCreate.builder().title("title").content("content").build();

        // when
        Post created = postService.createPost(writer, postCreate, null, "key-1");

        // then
        assertThat(created.getClientRequestId()).isEqualTo("key-1");
        assertThat(postService.findByClientRequestId(7L, "key-1").map(Post::getClientRequestId).orElse(null))
                .isEqualTo("key-1");
        assertThat(postService.findByClientRequestId(8L, "key-1").isPresent()).isFalse();
        assertThat(postService.findByClientRequestId(7L, "other").isPresent()).isFalse();
        assertThat(postService.findByClientRequestId(7L, null).isPresent()).isFalse();
        assertThat(postService.findByClientRequestId(1L, null).isPresent()).isFalse();
    }

    @Test
    void 지울_게시글이_이미_없으면_작성자_삭제는_조용히_끝난다() {
        // 작성자 확인은 통과했지만, 동시에 들어온 다른 삭제가 먼저 지운 상황(softDelete 0행)
        User writer = User.builder().id(1L).email("w@test.com").name("w").role(Role.GUEST)
                .provider(Provider.GOOGLE).providerId("w").build();
        FakePostRepository posts = new FakePostRepository() {
            @Override
            public int softDelete(long postId) {
                return 0;
            }
        };
        posts.save(Post.builder().id(1L).title("t").content("c").user(writer).build());
        PostServiceImpl service = PostServiceImpl.builder().postRepository(posts).build();

        assertThatCode(() -> service.deletePost(1L, writer)).doesNotThrowAnyException();
    }

    @Test
    void 없는_게시글을_차단해도_조용히_끝난다() {
        PostServiceImpl service = PostServiceImpl.builder().postRepository(new FakePostRepository()).build();

        assertThatCode(() -> service.blockPost(999L)).doesNotThrowAnyException();
    }

    @Test
    void updatePost는_수정하는_동안_바뀐_좋아요_수를_덮어쓰지_않는다() {
        // 수정 요청이 게시글을 읽은 직후 다른 요청이 좋아요 2개를 반영한 상황
        FakePostRepository racingPosts = new FakePostRepository() {
            private boolean first = true;

            @Override
            public Optional<Post> findById(Long id, Long userId) {
                Optional<Post> read = super.findById(id, userId);
                if (first) {
                    first = false;
                    incrementLikeCount(id);
                    incrementLikeCount(id);
                }
                return read;
            }
        };
        User author = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test_name")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .build();
        racingPosts.save(Post.builder().id(1L).title("title").content("content").user(author).build());
        PostServiceImpl service = PostServiceImpl.builder()
                .postRepository(racingPosts)
                .postImageRepository(new FakePostImageRepository())
                .build();

        Post result = service.updatePost(author, 1L, PostUpdate.builder().title("new title").build(),
                Collections.emptyList());

        assertThat(result.getTitle()).isEqualTo("new title");
        assertThat(result.getContent()).isEqualTo("content");
        assertThat(result.getLikeCount()).isEqualTo(2);
    }
}
