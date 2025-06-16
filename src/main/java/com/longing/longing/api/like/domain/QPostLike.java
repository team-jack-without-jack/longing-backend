package com.longing.longing.api.like.domain;

import com.longing.longing.api.like.infrastructure.PostLikeEntity;
import com.longing.longing.api.post.domain.QPost;
import com.longing.longing.api.user.domain.QUser;
import com.querydsl.core.types.dsl.EntityPathBase;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

public class QPostLike extends EntityPathBase<PostLikeEntity> {
    public static final QPostLike postLike = new QPostLike("postLike");

    public final QPost post;
    public final QUser user;

    public QPostLike(String variable) {
        super(PostLikeEntity.class, forVariable(variable));
        this.post = new QPost(forProperty("post")); // ✅ Post 객체와 연결
        this.user = new QUser(forProperty("user"));
    }
}
