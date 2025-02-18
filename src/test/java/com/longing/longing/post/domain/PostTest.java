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
//                .userId(1L)
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
//        assertThat(post.getCreatedAt()).isEqualTo(1678530673958L);
    }
}
