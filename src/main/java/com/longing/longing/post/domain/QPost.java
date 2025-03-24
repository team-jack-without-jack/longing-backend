package com.longing.longing.post.domain;

import com.longing.longing.post.infrastructure.PostEntity;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

import javax.annotation.processing.Generated;
import javax.management.Query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.Path;
import lombok.Getter;

/**
 * QPost is a Querydsl query type for Post
 */
@Getter
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPost extends EntityPathBase<PostEntity> {

    private static final long serialVersionUID = 1L;

    public static final QPost post = new QPost("post");

    public final NumberPath<Long> id = createNumber("id", Long.class);
    public final StringPath title = createString("title");
    public final StringPath content = createString("content");
//    public final BooleanPath isDeleted = createBoolean("isDeleted");
    public final BooleanPath deleted = createBoolean("deleted");

    public QPost(String variable) {
        super(PostEntity.class, forVariable(variable));
    }
    public QPost(Path<? extends PostEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPost(PathMetadata metadata) {
        super(PostEntity.class, metadata);
    }
}