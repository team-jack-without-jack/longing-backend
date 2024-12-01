package com.longing.longing.post.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.like.infrastructure.LikeEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;

import javax.persistence.*;
import java.util.List;

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
    public PostEntity(String title, String content, UserEntity user) {
        this.title = title;
        this.content = content;
        this.user = user;
    }
}
