package com.longing.longing.post.infrastructure;

import com.longing.longing.bookmark.infrastructure.PostBookmarkEntity;
import com.longing.longing.comment.infrastructure.CommentEntity;
import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.common.infrastructure.PostImageEntity;
import com.longing.longing.like.infrastructure.PostLikeEntity;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Table(name = "posts")
@SQLDelete(sql = "UPDATE posts SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
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

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostBookmarkEntity> postBookmarkEntities = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImageEntity> postImageEntities = new ArrayList<>();

    @Column(columnDefinition = "integer default 0")
    private int likeCount = 0;

    @Column(columnDefinition = "integer default 0")
    private int commentCount = 0;

    public PostEntity() {

    }

    @Builder
    public PostEntity(
            Long id,
            String title,
            String content,
            UserEntity user,
            int likeCount,
            int commentCount
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    public static PostEntity fromModel(Post post) {
        return PostEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .user(UserEntity.fromModel(post.getUser()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
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
