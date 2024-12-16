package com.longing.longing.user.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.like.infrastructure.LikeEntity;
import com.longing.longing.location.infrastructure.LocationEntity;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.Provider;
import com.longing.longing.user.Role;
import com.longing.longing.user.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.List;

@Slf4j
@Getter
@Entity
@Table(name = "users")
public class UserEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column
    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user")
    private List<PostEntity> postEntities;

    @OneToMany(mappedBy = "user")
    private List<LikeEntity> likeEntities;

    @OneToMany(mappedBy = "user")
    private List<CommentEntity> commentEntities;

    @OneToMany(mappedBy = "user")
    private List<LocationEntity> locationEntities;

    @Builder
    public UserEntity(
            Long id,
            String name,
            String email,
            String picture,
            Provider provider,
            String providerId,
            Role role
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    public UserEntity update(String name, String picture) {
        this.name = name;
        this.picture = picture;

        return this;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }

    public static UserEntity from(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .picture(user.getPicture())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .role(user.getRole())
                .build();
    }

    public User toModel() {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .picture(picture)
                .provider(provider)
                .providerId(providerId)
                .role(role)
                .build();
    }
}
