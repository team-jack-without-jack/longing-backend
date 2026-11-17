package com.longing.longing.bookmark.service;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.mock.FakeBookmarkRepository;
import com.longing.longing.mock.FakePostRepository;
import com.longing.longing.mock.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookmarkServiceTest {

    private static final long USER_ID = 1L;
    private static final long POST_ID = 10L;

    private FakeBookmarkRepository bookmarkRepository;
    private BookmarkServiceImpl bookmarkService;
    private CustomUserDetails member;

    @BeforeEach
    void init() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakePostRepository postRepository = new FakePostRepository();
        bookmarkRepository = new FakeBookmarkRepository();
        bookmarkService = new BookmarkServiceImpl(bookmarkRepository, userRepository, postRepository);

        User user = User.builder()
                .id(USER_ID)
                .email("member@example.com")
                .name("member")
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId("1")
                .build();
        userRepository.save(user);
        postRepository.save(Post.builder().id(POST_ID).title("title").content("content").user(user).build());
        member = new CustomUserDetails("member@example.com", Provider.GOOGLE);
    }

    @Test
    void 북마크를_추가하면_1행이_생긴다() {
        bookmarkService.addBookmark(member, POST_ID);

        assertThat(bookmarkRepository.count(USER_ID, POST_ID)).isEqualTo(1);
    }

    @Test
    void 이미_북마크했으면_저장하지_않고_성공한다() {
        bookmarkService.addBookmark(member, POST_ID);
        bookmarkService.addBookmark(member, POST_ID);

        assertThat(bookmarkRepository.count(USER_ID, POST_ID)).isEqualTo(1);
        assertThat(bookmarkRepository.getSaveIfAbsentCalls()).isEqualTo(1);
    }

    @Test
    void 북마크를_해제하면_행이_사라진다() {
        bookmarkService.addBookmark(member, POST_ID);

        bookmarkService.removeBookmark(member, POST_ID);

        assertThat(bookmarkRepository.count(USER_ID, POST_ID)).isZero();
    }

    @Test
    void 북마크가_없어도_해제는_성공한다() {
        bookmarkService.removeBookmark(member, POST_ID);

        assertThat(bookmarkRepository.count(USER_ID, POST_ID)).isZero();
    }

    @Test
    void 없는_게시글은_북마크할_수_없다() {
        assertThatThrownBy(() -> bookmarkService.addBookmark(member, 99L))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(bookmarkRepository.getSaveIfAbsentCalls()).isZero();
    }

    @Test
    void 없는_게시글의_북마크는_해제할_수_없다() {
        assertThatThrownBy(() -> bookmarkService.removeBookmark(member, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
