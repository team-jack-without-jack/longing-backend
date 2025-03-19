package com.longing.longing.common.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostImageEntity is a Querydsl query type for PostImageEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostImageEntity extends EntityPathBase<PostImageEntity> {

    private static final long serialVersionUID = 514420121L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostImageEntity postImageEntity = new QPostImageEntity("postImageEntity");

    public final com.longing.longing.common.QBaseTimeEntity _super = new com.longing.longing.common.QBaseTimeEntity(this);

    public final StringPath address = createString("address");

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

    public QPostImageEntity(String variable) {
        this(PostImageEntity.class, forVariable(variable), INITS);
    }

    public QPostImageEntity(Path<? extends PostImageEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostImageEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostImageEntity(PathMetadata metadata, PathInits inits) {
        this(PostImageEntity.class, metadata, inits);
    }

    public QPostImageEntity(Class<? extends PostImageEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new com.longing.longing.post.infrastructure.QPostEntity(forProperty("post"), inits.get("post")) : null;
        this.user = inits.isInitialized("user") ? new com.longing.longing.user.infrastructure.QUserEntity(forProperty("user")) : null;
    }

}

