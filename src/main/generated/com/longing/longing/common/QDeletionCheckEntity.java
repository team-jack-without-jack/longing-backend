package com.longing.longing.common;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDeletionCheckEntity is a Querydsl query type for DeletionCheckEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QDeletionCheckEntity extends EntityPathBase<DeletionCheckEntity> {

    private static final long serialVersionUID = -2073487785L;

    public static final QDeletionCheckEntity deletionCheckEntity = new QDeletionCheckEntity("deletionCheckEntity");

    public final BooleanPath deleted = createBoolean("deleted");

    public final DateTimePath<java.time.LocalDateTime> deletedDate = createDateTime("deletedDate", java.time.LocalDateTime.class);

    public QDeletionCheckEntity(String variable) {
        super(DeletionCheckEntity.class, forVariable(variable));
    }

    public QDeletionCheckEntity(Path<? extends DeletionCheckEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDeletionCheckEntity(PathMetadata metadata) {
        super(DeletionCheckEntity.class, metadata);
    }

}

