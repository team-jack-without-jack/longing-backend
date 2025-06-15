package com.longing.longing.api.user.infrastructure;

import com.longing.longing.api.location.infrastructure.LocationEntity;
import com.longing.longing.api.report.infrastructure.PostReportEntity;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.common.infrastructure.PostImageEntity;
import com.longing.longing.api.like.infrastructure.PostLikeEntity;
import com.longing.longing.api.post.infrastructure.PostEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
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

    @Column
    private String nationality;

    @Column
    private String introduction;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PostEntity> postEntities = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<PostLikeEntity> postLikeEntities = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<CommentEntity> commentEntities = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<LocationEntity> locationEntities = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<PostImageEntity> postImageEntities = new ArrayList<>();

    @OneToMany(mappedBy = "reporter")
    private List<PostReportEntity> postReportEntities = new ArrayList<>();

    // 기본 생성자 추가
    public UserEntity() {
        // 기본 생성자
    }

    @Builder
    public UserEntity(
            Long id,
            String name,
            String email,
            String picture,
            Provider provider,
            String providerId,
            Role role,
            String nationality,
            String introduction
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
        this.nationality = nationality;
        this.introduction = introduction;
    }

    public UserEntity update(
            String name,
            String nationality,
            String introduction,
            String picture
    ) {
        if (name != null) {
            this.name = name;
        }
        if (nationality != null) {
            this.nationality = nationality;
        }
        if (introduction != null) {
            this.introduction = introduction;
        }
        if (picture != null) {
            this.picture = picture;
        }
        return this;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }

    public static UserEntity fromModel(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .picture(user.getPicture())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .role(user.getRole())
                .nationality(user.getNationality())
                .introduction(user.getIntroduction())
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
                .nationality(nationality)
                .introduction(introduction)
                .build();
    }
}
