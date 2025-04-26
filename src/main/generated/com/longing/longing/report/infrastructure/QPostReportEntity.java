package com.longing.longing.report.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostReportEntity is a Querydsl query type for PostReportEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostReportEntity extends EntityPathBase<PostReportEntity> {

    private static final long serialVersionUID = -1046339483L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostReportEntity postReportEntity = new QPostReportEntity("postReportEntity");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.longing.longing.post.infrastructure.QPostEntity post;

    public final com.longing.longing.user.infrastructure.QUserEntity reporter;

    public final EnumPath<com.longing.longing.report.ReportReason> reportReason = createEnum("reportReason", com.longing.longing.report.ReportReason.class);

    public QPostReportEntity(String variable) {
        this(PostReportEntity.class, forVariable(variable), INITS);
    }

    public QPostReportEntity(Path<? extends PostReportEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostReportEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostReportEntity(PathMetadata metadata, PathInits inits) {
        this(PostReportEntity.class, metadata, inits);
    }

    public QPostReportEntity(Class<? extends PostReportEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new com.longing.longing.post.infrastructure.QPostEntity(forProperty("post"), inits.get("post")) : null;
        this.reporter = inits.isInitialized("reporter") ? new com.longing.longing.user.infrastructure.QUserEntity(forProperty("reporter")) : null;
    }

}

