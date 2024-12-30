package com.longing.longing.post.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "posts")
public class PostEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @OneToMany(mappedBy = "post")
    private List<CommentEntity> commentEntities = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<PostLikeEntity> postLikeEntities = new ArrayList<>();

    @Transient
    private int likeCount = 0;

    public PostEntity() {

    }

    @Builder
    public PostEntity(String title, String content, UserEntity user) {
        this.title = title;
        this.content = content;
        this.user = user;
    }

    public static PostEntity fromModel(Post post) {
        return PostEntity.builder()
                .title(post.getTitle())
                .content(post.getContent())
                .user(UserEntity.fromModel(post.getUser()))
                .build();
    }

    public Post toModel() {
        return Post.builder()
                .id(id)
                .title(title)
                .content(content)
                .user(user.toModel())
                .build();
    }

    public int getLikeCount() {
        return postLikeEntities.size();  // 실시간으로 계산
    }

    public PostEntity update(PostUpdate postUpdate) {
        if (postUpdate.getTitle() != null) {
            this.title = postUpdate.getTitle();
        }
        if (postUpdate.getContent() != null) {
            this.content = postUpdate.getContent();
        }
        return this;
    }
}
