package com.longing.longing.api.user.service;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.mock.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserBlockServiceTest {

    private FakeUserRepository userRepository;
    private UserServiceImpl userService;
    private User member;
    private User other;

    @BeforeEach
    void init() {
        userRepository = new FakeUserRepository();
        userService = UserServiceImpl.builder()
                .userRepository(userRepository)
                .build();
        member = userRepository.save(user(1L, "member@example.com"));
        other = userRepository.save(user(2L, "other@example.com"));
    }

    @Test
    void 사용자를_차단하면_차단_관계가_생긴다() {
        userService.blockUser(member, other.getId());

        assertThat(userRepository.isBlocked(member.getId(), other.getId())).isTrue();
    }

    @Test
    void 이미_차단했으면_저장하지_않고_성공한다() {
        userService.blockUser(member, other.getId());
        userService.blockUser(member, other.getId());

        assertThat(userRepository.isBlocked(member.getId(), other.getId())).isTrue();
        assertThat(userRepository.getBlockIfAbsentCalls()).isEqualTo(1);
    }

    @Test
    void 차단을_해제하면_차단_관계가_사라진다() {
        userService.blockUser(member, other.getId());

        userService.cancelBlockUser(member, other.getId());

        assertThat(userRepository.isBlocked(member.getId(), other.getId())).isFalse();
    }

    @Test
    void 차단하지_않았어도_해제는_성공한다() {
        assertThatCode(() -> userService.cancelBlockUser(member, other.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void 없는_사용자는_차단할_수_없다() {
        assertThatThrownBy(() -> userService.blockUser(member, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(userRepository.getBlockIfAbsentCalls()).isZero();
    }

    @Test
    void 없는_사용자는_차단_해제할_수_없다() {
        assertThatThrownBy(() -> userService.cancelBlockUser(member, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static User user(long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .name(email)
                .role(Role.GUEST)
                .provider(Provider.GOOGLE)
                .providerId(String.valueOf(id))
                .build();
    }
}
