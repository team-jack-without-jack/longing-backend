package com.longing.longing.user.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.like.infrastructure.LikeEntity;
import com.longing.longing.location.infrastructure.LocationEntity;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.Provider;
import com.longing.longing.user.Role;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;
import java.util.List;

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
    public UserEntity(String name, String email, String picture, Provider provider, Role role) {
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
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
}
