package com.longing.longing.post.domain;

import com.longing.longing.user.Provider;
import com.longing.longing.user.Role;
import com.longing.longing.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostTest {
    @Test
    public void PostCreate으로_게시물을_만들_수_있다() {
        // given
        PostCreate postCreate = PostCreate.builder()
                .title("test title")
                .content("hello world")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("test001")
                .role(Role.GUEST)
                .provider(Provider.KAKAO)
                .providerId("123")
                .build();

        // when
//        Post post = Post.from(writer, postCreate, new TestClockHolder(1678530673958L));
        Post post = Post.from(user, postCreate);

        // then
        assertThat(post.getContent()).isEqualTo("hello world");
        assertThat(post.getTitle()).isEqualTo("test title");
        assertThat(post.getUser().getEmail()).isEqualTo("test@test.com");
        assertThat(post.getUser().getName()).isEqualTo("test001");
        assertThat(post.getUser().getRole()).isEqualTo(Role.GUEST);
        assertThat(post.getUser().getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(post.getUser().getProviderId()).isEqualTo("123");
        assertThat(post.getLikeCount()).isEqualTo(0);
        assertThat(post.getCommentCount()).isEqualTo(0);

        assertThat(post.isBookmarked()).isFalse();
        assertThat(post.isLiked()).isFalse();

//        assertThat(post.getCreatedAt()).isEqualTo(1678530673958L);
    }

    @Test
    public void PostUpdate로_포스트를_수정할_수_있다() {
        // given
        PostUpdate postUpdate = PostUpdate.builder()
                .title("update_title")
                .content("update_content")
                .build();

        User writer = User.builder()
                .email("user@test.com")
                .name("update_name")
                .picture("update_picture")
                .provider(Provider.GOOGLE)
                .providerId("1")
                .role(Role.GUEST)
                .nationality("KOREA")
                .introduction("update_introduction")
                .build();

        Post post = Post.builder()
                .id(1L)
                .title("test_title")
                .content("test_content")
                .user(writer)
                .build();
        // when
        post = post.update(postUpdate);

        // then
        assertThat(post.getContent()).isEqualTo("update_content");
        assertThat(post.getUser().getEmail()).isEqualTo("user@test.com");
        assertThat(post.getUser().getName()).isEqualTo("update_name");
        assertThat(post.getUser().getPicture()).isEqualTo("update_picture");
        assertThat(post.getUser().getProviderId()).isEqualTo("1");
        assertThat(post.getUser().getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(post.getUser().getRole()).isEqualTo(Role.GUEST);
        assertThat(post.getUser().getNationality()).isEqualTo("KOREA");
        assertThat(post.getUser().getIntroduction()).isEqualTo("update_introduction");
    }

    @Test
    public void 좋아요를_누르면_likeCount_가_1_증가한다() {
        // given
        Post post = Post.builder()
                .title("test_title")
                .content("test_content")
                .build();

        // when
        post.like();

        // then
        assertThat(post.getLikeCount()).isEqualTo(1);
    }

    @Test
    public void 좋아요를_한_번_더_누르면_likeCount_가_1_감소한다() {
        // given
        Post post = Post.builder()
                .title("test_title")
                .content("test_content")
                .likeCount(1)
                .build();

        // when
        post.unlike();

        // then
        assertThat(post.getLikeCount()).isEqualTo(0);
    }

    @Test
    public void 좋아요개수가_0인_경우_unlike_를_누르면_그대로_0이다() {
        // given
        Post post = Post.builder()
                .title("test_title")
                .content("test_content")
                .likeCount(0)
                .build();

        // when
        post.unlike();

        // then
        assertThat(post.getLikeCount()).isEqualTo(0);
    }
}
