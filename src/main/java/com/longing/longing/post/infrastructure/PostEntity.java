package com.longing.longing.post.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<CommentEntity> commentEntities = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostLikeEntity> postLikeEntities = new ArrayList<>();

    @Column(columnDefinition = "integer default 0")
    private int likeCount = 0;

    public PostEntity() {

    }

    @Builder
    public PostEntity(Long id, String title, String content, UserEntity user, int likeCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
        this.likeCount = likeCount;
    }

    public static PostEntity fromModel(Post post) {
        return PostEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .user(UserEntity.fromModel(post.getUser()))
                .likeCount(post.getLikeCount())
                .build();
    }

    public Post toModel() {
        return Post.builder()
                .id(id)
                .title(title)
                .content(content)
                .user(user.toModel())
                .likeCount(likeCount)
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

    // 연관 관계 관리 메서드
    public void like() {
        this.likeCount++;
    }

    public void unlike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void setUser(UserEntity user) {
        if (this.user != null) {
            this.user.getPostEntities().remove(this);
        }
        this.user = user;
        user.getPostEntities().add(this);
    }
}
