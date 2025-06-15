package com.longing.longing.bookmark.domain;

import com.longing.longing.bookmark.infrastructure.PostBookmarkEntity;
import com.longing.longing.api.post.domain.QPost;
import com.longing.longing.api.user.domain.QUser;
import com.querydsl.core.types.dsl.EntityPathBase;

import javax.annotation.processing.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * QPostBookmark is a Querydsl query type for PostBookmark
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPostBookmark extends EntityPathBase<PostBookmarkEntity> {

    public static final QPostBookmark postBookmark = new QPostBookmark("postBookmark");

    public final QPost post;  // ✅ post 필드 정의 (이 부분 확인)
    public final QUser user;

    public QPostBookmark(String variable) {
        super(PostBookmarkEntity.class, forVariable(variable));
        this.post = new QPost(forProperty("post")); // ✅ Post 객체와 연결
        this.user = new QUser(forProperty("user"));
    }
//    private static final long serialVersionUID = 1L;
//
//    public static final QPostBookmark postBookmark = new QPostBookmark("postBookmark");
//
//    public final NumberPath<Long> id = createNumber("id", Long.class);
//    public final QUser user = new QUser("user");  // User 관계 매핑
//    public final QPost post = new QPost("post");  // Post 관계 매핑
//
//    public QPostBookmark(String variable) {
//        super(PostBookmarkEntity.class, forVariable(variable));
//    }
//
//    public QPostBookmark(Path<? extends PostBookmarkEntity> path) {
//        super(path.getType(), path.getMetadata());
//    }
//
//    public QPostBookmark(PathMetadata metadata) {
//        super(PostBookmarkEntity.class, metadata);
//    }
}