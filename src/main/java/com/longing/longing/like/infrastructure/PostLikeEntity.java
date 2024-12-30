package com.longing.longing.like.infrastructure;

import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.like.domain.LikePost;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "post_likes")
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

    public static PostLikeEntity fromModel(LikePost likePost) {
        return PostLikeEntity.builder()
                .post(PostEntity.fromModel(likePost.getPost()))
                .user(UserEntity.fromModel(likePost.getUser()))
                .build();
    }

    public LikePost toModel() {
        return LikePost.builder()
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
