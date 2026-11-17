package com.longing.longing.bookmark.infrastructure;

import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.user.infrastructure.UserEntity;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

@Slf4j
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Hibernate에서 기본 생성자 필요
@AllArgsConstructor
@Table(name = "post_bookmarks", uniqueConstraints = @UniqueConstraint(
        name = PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT, columnNames = {"user_id", "post_id"}))
public class PostBookmarkEntity extends BaseTimeEntity {
    /** 운영 SQL(scripts/sql/124-bookmark-block-unique.sql)과 같은 이름이어야 한다 (#124). */
    public static final String USER_POST_UNIQUE_CONSTRAINT = "uk_post_bookmarks_user_post";

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
}
