package com.longing.longing.common.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLocationImageEntity is a Querydsl query type for LocationImageEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLocationImageEntity extends EntityPathBase<LocationImageEntity> {

    private static final long serialVersionUID = 1833009636L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLocationImageEntity locationImageEntity = new QLocationImageEntity("locationImageEntity");

    public final com.longing.longing.common.QBaseTimeEntity _super = new com.longing.longing.common.QBaseTimeEntity(this);

    public final StringPath address = createString("address");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final BooleanPath deleted = _super.deleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isThumbnail = createBoolean("isThumbnail");

    public final com.longing.longing.location.infrastructure.QLocationEntity location;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final com.longing.longing.user.infrastructure.QUserEntity user;

    public QLocationImageEntity(String variable) {
        this(LocationImageEntity.class, forVariable(variable), INITS);
    }

    public QLocationImageEntity(Path<? extends LocationImageEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLocationImageEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLocationImageEntity(PathMetadata metadata, PathInits inits) {
        this(LocationImageEntity.class, metadata, inits);
    }

    public QLocationImageEntity(Class<? extends LocationImageEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.location = inits.isInitialized("location") ? new com.longing.longing.location.infrastructure.QLocationEntity(forProperty("location"), inits.get("location")) : null;
        this.user = inits.isInitialized("user") ? new com.longing.longing.user.infrastructure.QUserEntity(forProperty("user")) : null;
    }

}

