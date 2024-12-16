package com.longing.longing.post.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.like.infrastructure.LikeEntity;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;
import java.util.List;

@Getter
@Entity
@Table(name = "posts")
public class PostEntity {
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
    private List<CommentEntity> commentEntities;

    @OneToMany(mappedBy = "post")
    private List<LikeEntity> likeEntities;

    @Builder
    public PostEntity(Long id, String title, String content, UserEntity user) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
    }

    public static PostEntity fromModel(Post post) {
//        PostEntity postEntity = new PostEntity();
//        postEntity.id = post.getId();
//        postEntity.content = post.getContent();
//        postEntity.createdAt = post.getCreatedAt();
//        postEntity.modifiedAt = post.getModifiedAt();
//        postEntity.writer = UserEntity.from(post.getWriter());
//        return postEntity;
        return PostEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .user(UserEntity.from(post.getUser()))
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
}
