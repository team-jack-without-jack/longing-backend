package com.longing.longing.comment.infrastructure;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "comments")
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Builder
    public CommentEntity(Long id, String content, PostEntity post, UserEntity user) {
        this.id = id;
        this.content = content;
        this.post = post;
        this.user = user;
    }

    public static CommentEntity fromModel(Comment comment) {
        return CommentEntity.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .post(PostEntity.fromModel(comment.getPost()))
                .user(UserEntity.fromModel(comment.getUser()))
                .build();
    }

    public Comment toModel() {
        return Comment.builder()
                .id(id)
                .content(content)
                .post(post.toModel())
                .user(user.toModel())
                .build();
    }
}
