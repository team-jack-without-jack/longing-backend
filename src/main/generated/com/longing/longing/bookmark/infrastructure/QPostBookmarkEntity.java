package com.longing.longing.bookmark.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.longing.longing.api.bookmark.infrastructure.PostBookmarkEntity;
import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostBookmarkEntity is a Querydsl query type for PostBookmarkEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostBookmarkEntity extends EntityPathBase<PostBookmarkEntity> {

    private static final long serialVersionUID = -709828695L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostBookmarkEntity postBookmarkEntity = new QPostBookmarkEntity("postBookmarkEntity");

    public final com.longing.longing.common.QBaseTimeEntity _super = new com.longing.longing.common.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final BooleanPath deleted = _super.deleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final com.longing.longing.post.infrastructure.QPostEntity post;

    public final com.longing.longing.user.infrastructure.QUserEntity user;

    public QPostBookmarkEntity(String variable) {
        this(PostBookmarkEntity.class, forVariable(variable), INITS);
    }

    public QPostBookmarkEntity(Path<? extends PostBookmarkEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostBookmarkEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostBookmarkEntity(PathMetadata metadata, PathInits inits) {
        this(PostBookmarkEntity.class, metadata, inits);
    }

    public QPostBookmarkEntity(Class<? extends PostBookmarkEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new com.longing.longing.post.infrastructure.QPostEntity(forProperty("post"), inits.get("post")) : null;
        this.user = inits.isInitialized("user") ? new com.longing.longing.user.infrastructure.QUserEntity(forProperty("user")) : null;
    }

}

