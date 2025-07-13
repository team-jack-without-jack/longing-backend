package com.longing.longing.api.user.infrastructure;

import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Slf4j
@Getter
@Entity
@Table(name = "user_blocks")
@SQLDelete(sql = "UPDATE user_blocks SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class UserBlockEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity blockedUser;

    @Builder
    public UserBlockEntity (
            UserEntity user,
            UserEntity blockedUser
    ) {
        this.user = user;
        this.blockedUser = blockedUser;
    }

    public static UserBlockEntity fromModel(
            User user,
            User blockedUser
    ) {
        return UserBlockEntity.builder()
                .user(UserEntity.fromModel(user))
                .blockedUser(UserEntity.fromModel(blockedUser))
                .build();
    }

}
