package com.longing.longing.comment.infrastructure;

import com.longing.longing.comment.domain.Comment;
import com.longing.longing.comment.domain.CommentUpdate;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
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

//    @OneToMany(mappedBy = "comment")
//    private List<CommentLikeEntity> commentLikeEntities = new ArrayList<>();


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

    public CommentEntity update(CommentUpdate commentUpdate) {
        if (commentUpdate.getContent() != null) {
            this.content = commentUpdate.getContent();
        }

        return this;
    }

    public void setPost(PostEntity post) {
        if (this.post != null) {
            this.post.getCommentEntities().remove(this);
        }
        this.post = post;
        post.getCommentEntities().add(this);
    }

    public void setUser(UserEntity user) {
        if (this.user != null) {
            this.user.getCommentEntities().remove(this);
        }
        this.user = user;
        user.getCommentEntities().add(this);
    }
}
