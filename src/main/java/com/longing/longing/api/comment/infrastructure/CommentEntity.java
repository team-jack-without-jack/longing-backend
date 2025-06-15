package com.longing.longing.api.comment.infrastructure;

import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE comments SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class CommentEntity extends BaseTimeEntity {

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
