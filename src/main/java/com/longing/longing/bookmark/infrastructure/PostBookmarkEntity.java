package com.longing.longing.bookmark.infrastructure;

import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

@Slf4j
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Hibernate에서 기본 생성자 필요
@AllArgsConstructor
@Table(name = "post_bookmarks")
public class PostBookmarkEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    public static PostBookmarkEntity fromModel(
            PostEntity postEntity,
            UserEntity userEntity
    ) {
        return PostBookmarkEntity.builder()
                .post(postEntity)
                .user(userEntity)
                .build();
    }

    public void addBookmark() {
        if (this.post != null) {
            this.post.getPostBookmarkEntities().add(this);
        }
    }

    public void removeBookmark() {
        if (this.post != null) {
            this.post.getPostBookmarkEntities().remove(this);
        }
    }

    public PostBookmark toModel() {
        return PostBookmark.builder()
                .id(id)
                .post(post.toModel())
                .user(user.toModel())
                .build();
    }
}
