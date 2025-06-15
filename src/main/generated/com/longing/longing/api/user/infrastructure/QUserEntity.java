package com.longing.longing.api.user.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserEntity is a Querydsl query type for UserEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserEntity extends EntityPathBase<UserEntity> {

    private static final long serialVersionUID = -613037465L;

    public static final QUserEntity userEntity = new QUserEntity("userEntity");

    public final com.longing.longing.common.QBaseTimeEntity _super = new com.longing.longing.common.QBaseTimeEntity(this);

    public final ListPath<com.longing.longing.api.comment.infrastructure.CommentEntity, com.longing.longing.api.comment.infrastructure.QCommentEntity> commentEntities = this.<com.longing.longing.api.comment.infrastructure.CommentEntity, com.longing.longing.api.comment.infrastructure.QCommentEntity>createList("commentEntities", com.longing.longing.api.comment.infrastructure.CommentEntity.class, com.longing.longing.api.comment.infrastructure.QCommentEntity.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final BooleanPath deleted = _super.deleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath introduction = createString("introduction");

    public final ListPath<com.longing.longing.api.location.infrastructure.LocationEntity, com.longing.longing.api.location.infrastructure.QLocationEntity> locationEntities = this.<com.longing.longing.api.location.infrastructure.LocationEntity, com.longing.longing.api.location.infrastructure.QLocationEntity>createList("locationEntities", com.longing.longing.api.location.infrastructure.LocationEntity.class, com.longing.longing.api.location.infrastructure.QLocationEntity.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath name = createString("name");

    public final StringPath nationality = createString("nationality");

    public final StringPath picture = createString("picture");

    public final ListPath<com.longing.longing.api.post.infrastructure.PostEntity, com.longing.longing.api.post.infrastructure.QPostEntity> postEntities = this.<com.longing.longing.api.post.infrastructure.PostEntity, com.longing.longing.api.post.infrastructure.QPostEntity>createList("postEntities", com.longing.longing.api.post.infrastructure.PostEntity.class, com.longing.longing.api.post.infrastructure.QPostEntity.class, PathInits.DIRECT2);

    public final ListPath<com.longing.longing.common.infrastructure.PostImageEntity, com.longing.longing.common.infrastructure.QPostImageEntity> postImageEntities = this.<com.longing.longing.common.infrastructure.PostImageEntity, com.longing.longing.common.infrastructure.QPostImageEntity>createList("postImageEntities", com.longing.longing.common.infrastructure.PostImageEntity.class, com.longing.longing.common.infrastructure.QPostImageEntity.class, PathInits.DIRECT2);

    public final ListPath<com.longing.longing.api.like.infrastructure.PostLikeEntity, com.longing.longing.api.like.infrastructure.QPostLikeEntity> postLikeEntities = this.<com.longing.longing.api.like.infrastructure.PostLikeEntity, com.longing.longing.api.like.infrastructure.QPostLikeEntity>createList("postLikeEntities", com.longing.longing.api.like.infrastructure.PostLikeEntity.class, com.longing.longing.api.like.infrastructure.QPostLikeEntity.class, PathInits.DIRECT2);

    public final ListPath<com.longing.longing.api.report.infrastructure.PostReportEntity, com.longing.longing.api.report.infrastructure.QPostReportEntity> postReportEntities = this.<com.longing.longing.api.report.infrastructure.PostReportEntity, com.longing.longing.api.report.infrastructure.QPostReportEntity>createList("postReportEntities", com.longing.longing.api.report.infrastructure.PostReportEntity.class, com.longing.longing.api.report.infrastructure.QPostReportEntity.class, PathInits.DIRECT2);

    public final EnumPath<com.longing.longing.api.user.Provider> provider = createEnum("provider", com.longing.longing.api.user.Provider.class);

    public final StringPath providerId = createString("providerId");

    public final EnumPath<com.longing.longing.api.user.Role> role = createEnum("role", com.longing.longing.api.user.Role.class);

    public QUserEntity(String variable) {
        super(UserEntity.class, forVariable(variable));
    }

    public QUserEntity(Path<? extends UserEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserEntity(PathMetadata metadata) {
        super(UserEntity.class, metadata);
    }

}

