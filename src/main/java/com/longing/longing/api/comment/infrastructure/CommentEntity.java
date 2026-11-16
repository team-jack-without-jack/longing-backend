package com.longing.longing.api.comment.infrastructure;

import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.user.infrastructure.UserEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "comments", uniqueConstraints = @UniqueConstraint(
        name = CommentEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT,
        columnNames = {"user_id", "client_request_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE comments SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class CommentEntity extends BaseTimeEntity {

    /** scripts/sql/122-create-dedupe.sql과 같은 이름. 중복 키 위반 판별에 사용한다. */
    public static final String CLIENT_REQUEST_UNIQUE_CONSTRAINT = "uk_comments_user_client_request";

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

    /** Redis 키처럼 대소문자를 구분하도록 바이너리 정렬(MariaDB 기본 _ci 정렬은 abc와 ABC를 같게 본다). */
    @Column(name = "client_request_id", columnDefinition = "VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin")
    private String clientRequestId;

//    @OneToMany(mappedBy = "comment")
//    private List<CommentLikeEntity> commentLikeEntities = new ArrayList<>();


    @Builder
    public CommentEntity(Long id, String content, PostEntity post, UserEntity user, String clientRequestId) {
        this.id = id;
        this.content = content;
        this.post = post;
        this.user = user;
        this.clientRequestId = clientRequestId;
    }

    public static CommentEntity fromModel(Comment comment) {
        return CommentEntity.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .post(PostEntity.fromModel(comment.getPost()))
                .user(UserEntity.fromModel(comment.getUser()))
                .clientRequestId(comment.getClientRequestId())
                .build();
    }

    public Comment toModel() {
        return Comment.builder()
                .id(id)
                .content(content)
                .post(post.toModel())
                .user(user.toModel())
                .createdDate(getCreatedDate())
                .modifiedDate(getModifiedDate())
                .clientRequestId(clientRequestId)
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
