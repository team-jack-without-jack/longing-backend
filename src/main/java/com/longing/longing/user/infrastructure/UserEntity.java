package com.longing.longing.user.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.common.infrastructure.PostImageEntity;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.location.infrastructure.LocationEntity;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.Provider;
import com.longing.longing.user.Role;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.domain.UserUpdate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.web.multipart.MultipartFile;

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
