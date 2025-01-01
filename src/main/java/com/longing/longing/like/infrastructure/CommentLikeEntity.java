package com.longing.longing.like.infrastructure;

import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.like.domain.LikeCommentCreate;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "comment_likes")
public class CommentLikeEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private CommentEntity comment;

    @Builder
    public CommentLikeEntity(UserEntity user, CommentEntity comment) {
        this.user = user;
        this.comment = comment;
    }

    public static CommentLikeEntity fromModel(LikeCommentCreate likeComment) {
        return CommentLikeEntity.builder()
                .comment(CommentEntity.fromModel(likeComment.getComment()))
                .user(UserEntity.fromModel(likeComment.getUser()))
                .build();
    }

    public LikeCommentCreate toModel() {
        return LikeCommentCreate.builder()
                .id(id)
                .comment(comment.toModel())
                .user(user.toModel())
                .build();
    }

}
