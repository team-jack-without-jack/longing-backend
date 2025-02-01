package com.longing.longing.common.infrastructure;

import com.longing.longing.common.domain.PostImage;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "post_images")
public class PostImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Builder
    public PostImageEntity(
            Long id,
            String address,
            PostEntity post,
            UserEntity user
    ) {
        this.id = id;
        this.address = address;
        this.post = post;
        this.user = user;
    }

    @Builder
    public PostImageEntity(String address, PostEntity post, UserEntity user) {
        this.address = address;
        this.post = post;
        this.user = user;
    }


    public static PostImageEntity fromModel(PostImage postImage) {
        return PostImageEntity.builder()
                .id(postImage.getId())
                .address(postImage.getAddress())
                .post(PostEntity.fromModel(postImage.getPost()))
                .user(UserEntity.fromModel(postImage.getUser()))
                .build();
    }

    public PostImage toModel() {
        return PostImage.builder()
                .id(id)
                .address(address)
//                .post(post.toModel())
//                .user(user.toModel())
                .build();
    }
}
