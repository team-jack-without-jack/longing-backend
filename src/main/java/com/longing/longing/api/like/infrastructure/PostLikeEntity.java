package com.longing.longing.api.like.infrastructure;

import com.longing.longing.api.like.domain.PostLike;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.common.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;

@Getter
@Entity
@Builder
@AllArgsConstructor
@Table(name = "post_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @Builder
    public PostLikeEntity(UserEntity user, PostEntity post) {
        this.user = user;
        this.post = post;
    }

    public static PostLikeEntity fromModel(
            PostEntity postEntity,
            UserEntity userEntity
    ) {
        return PostLikeEntity.builder()
                .post(postEntity)
                .user(userEntity)
                .build();
    }

    public PostLike toModel() {
        return PostLike.builder()
                .id(id)
                .post(post.toModel())
                .user(user.toModel())
                .build();
    }

    public void likePost() {
        if (this.post != null) {
            this.post.getPostLikeEntities().add(this);
        }
    }

    public void unlikePost() {
        if (this.post != null) {
            this.post.getPostLikeEntities().remove(this);
        }
    }

}
