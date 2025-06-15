package com.longing.longing.location.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.longing.longing.api.location.infrastructure.LocationEntity;
import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLocationEntity is a Querydsl query type for LocationEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLocationEntity extends EntityPathBase<LocationEntity> {

    private static final long serialVersionUID = -105816985L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLocationEntity locationEntity = new QLocationEntity("locationEntity");

    public final com.longing.longing.common.QBaseTimeEntity _super = new com.longing.longing.common.QBaseTimeEntity(this);

    public final StringPath address = createString("address");

    public final com.longing.longing.category.infrastructure.QCategoryEntity category;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final BooleanPath deleted = _super.deleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath mapUrl = createString("mapUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath name = createString("name");

    public final StringPath phoneNumber = createString("phoneNumber");

    public final com.longing.longing.user.infrastructure.QUserEntity user;

    public QLocationEntity(String variable) {
        this(LocationEntity.class, forVariable(variable), INITS);
    }

    public QLocationEntity(Path<? extends LocationEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLocationEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLocationEntity(PathMetadata metadata, PathInits inits) {
        this(LocationEntity.class, metadata, inits);
    }

    public QLocationEntity(Class<? extends LocationEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new com.longing.longing.category.infrastructure.QCategoryEntity(forProperty("category")) : null;
        this.user = inits.isInitialized("user") ? new com.longing.longing.user.infrastructure.QUserEntity(forProperty("user")) : null;
    }

}

