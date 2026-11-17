package com.longing.longing.api.post.infrastructure;

import com.longing.longing.api.comment.infrastructure.CommentEntity;
import com.longing.longing.api.report.infrastructure.PostReportEntity;
import com.longing.longing.bookmark.infrastructure.PostBookmarkEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.common.infrastructure.PostImageEntity;
import com.longing.longing.api.like.infrastructure.PostLikeEntity;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostUpdate;
import com.longing.longing.api.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Entity
@Table(name = "posts", uniqueConstraints = @UniqueConstraint(
        name = PostEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT,
        columnNames = {"user_id", "client_request_id"}))
@SQLDelete(sql = "UPDATE posts SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class PostEntity extends BaseTimeEntity {
    /** scripts/sql/122-create-dedupe.sql과 같은 이름. 중복 키 위반 판별에 사용한다. */
    public static final String CLIENT_REQUEST_UNIQUE_CONSTRAINT = "uk_posts_user_client_request";

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

    // 삭제는 PostRepository.softDelete가 게시글 행을 먼저 잠근 뒤 한 문장씩 처리한다 (#128).
    // cascade를 두지 않는다. em.remove의 cascade가 신고까지 하드 삭제했고, 자식 행부터 잠가 교착 상태를 만들었다.
    @OneToMany(mappedBy = "post")
    private List<CommentEntity> commentEntities = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<PostLikeEntity> postLikeEntities = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<PostBookmarkEntity> postBookmarkEntities = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<PostImageEntity> postImageEntities = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<PostReportEntity> postReportEntities = new ArrayList<>();

    @Column(columnDefinition = "integer default 0")
    private int likeCount = 0;

    @Column(columnDefinition = "integer default 0")
    private int commentCount = 0;

    /** Redis 키처럼 대소문자를 구분하도록 바이너리 정렬(MariaDB 기본 _ci 정렬은 abc와 ABC를 같게 본다). */
    @Column(name = "client_request_id", columnDefinition = "VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin")
    private String clientRequestId;

    public PostEntity() {

    }

    @Builder
    public PostEntity(
            Long id,
            String title,
            String content,
            UserEntity user,
            int likeCount,
            int commentCount,
            String clientRequestId
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.clientRequestId = clientRequestId;
    }

    public static PostEntity fromModel(Post post) {
        return PostEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .user(UserEntity.fromModel(post.getUser()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .clientRequestId(post.getClientRequestId())
                .build();
    }

    public Post toModel() {
        return Post.builder()
                .id(id)
                .title(title)
                .content(content)
                .user(user.toModel())
                .postImageList(postImageEntities.stream()
                        .map(PostImageEntity::toModel) // 변환 메서드 적용
                        .collect(Collectors.toList()))
                .likeCount(likeCount)
                .commentCount(commentCount)
                .createdDate(getCreatedDate())
                .modifiedDate(getModifiedDate())
                .clientRequestId(clientRequestId)
                .build();
    }

    public Post toModel(Boolean bookmarked, Boolean liked) {
        return Post.builder()
                .id(id)
                .title(title)
                .content(content)
                .user(user.toModel())
                .postImageList(postImageEntities.stream()
                        .map(PostImageEntity::toModel) // 변환 메서드 적용
                        .collect(Collectors.toList()))
                .likeCount(likeCount)
                .commentCount(commentCount)
                .bookmarked(bookmarked)
                .liked(liked)
                .createdDate(getCreatedDate())
                .modifiedDate(getModifiedDate())
                .clientRequestId(clientRequestId)
                .build();
    }

    public int getLikeCount() {
        return postLikeEntities.size();  // 실시간으로 계산
    }

//    public PostEntity update(PostUpdate postUpdate) {
//        if (postUpdate.getTitle() != null) {
//            this.title = postUpdate.getTitle();
//        }
//        if (postUpdate.getContent() != null) {
//            this.content = postUpdate.getContent();
//        }
//        return this;
//    }

    public PostEntity update(PostUpdate postUpdate) {
        updateField(postUpdate.getTitle(), value -> this.title = value);
        updateField(postUpdate.getContent(), value -> this.content = value);

        return this;
    }

    private <T> void updateField(T value, Consumer<T> updater) {
        if (value != null) {
            updater.accept(value);
        }
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

    //== 연관관계 편의 메서드 ==//
    public void addComment(CommentEntity comment) {
        commentEntities.add(comment);
        comment.setPost(this);
        this.commentCount++;  // 댓글 개수 증가
    }

    public void removeComment(CommentEntity comment) {
        commentEntities.remove(comment);
        this.commentCount--;  // 댓글 개수 감소
    }

    public void setUser(UserEntity user) {
        if (this.user != null) {
            this.user.getPostEntities().remove(this);
        }
        this.user = user;
        user.getPostEntities().add(this);
    }

    public void addImage(PostImageEntity image, UserEntity user) {
        this.postImageEntities.add(new PostImageEntity(image.getAddress(), this, user));
    }
}
