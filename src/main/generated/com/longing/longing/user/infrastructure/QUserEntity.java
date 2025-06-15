package com.longing.longing.user.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.longing.longing.api.comment.infrastructure.CommentEntity;
import com.longing.longing.api.like.infrastructure.PostLikeEntity;
import com.longing.longing.api.location.infrastructure.LocationEntity;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.report.infrastructure.PostReportEntity;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.infrastructure.UserEntity;
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

    private static final long serialVersionUID = -2030673261L;

    public static final QUserEntity userEntity = new QUserEntity("userEntity");

    public final com.longing.longing.common.QBaseTimeEntity _super = new com.longing.longing.common.QBaseTimeEntity(this);

    public final ListPath<CommentEntity, com.longing.longing.comment.infrastructure.QCommentEntity> commentEntities = this.<CommentEntity, com.longing.longing.comment.infrastructure.QCommentEntity>createList("commentEntities", CommentEntity.class, com.longing.longing.comment.infrastructure.QCommentEntity.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final BooleanPath deleted = _super.deleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath introduction = createString("introduction");

    public final ListPath<LocationEntity, com.longing.longing.location.infrastructure.QLocationEntity> locationEntities = this.<LocationEntity, com.longing.longing.location.infrastructure.QLocationEntity>createList("locationEntities", LocationEntity.class, com.longing.longing.location.infrastructure.QLocationEntity.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath name = createString("name");

    public final StringPath nationality = createString("nationality");

    public final StringPath picture = createString("picture");

    public final ListPath<PostEntity, com.longing.longing.post.infrastructure.QPostEntity> postEntities = this.<PostEntity, com.longing.longing.post.infrastructure.QPostEntity>createList("postEntities", PostEntity.class, com.longing.longing.post.infrastructure.QPostEntity.class, PathInits.DIRECT2);

    public final ListPath<com.longing.longing.common.infrastructure.PostImageEntity, com.longing.longing.common.infrastructure.QPostImageEntity> postImageEntities = this.<com.longing.longing.common.infrastructure.PostImageEntity, com.longing.longing.common.infrastructure.QPostImageEntity>createList("postImageEntities", com.longing.longing.common.infrastructure.PostImageEntity.class, com.longing.longing.common.infrastructure.QPostImageEntity.class, PathInits.DIRECT2);

    public final ListPath<PostLikeEntity, com.longing.longing.like.infrastructure.QPostLikeEntity> postLikeEntities = this.<PostLikeEntity, com.longing.longing.like.infrastructure.QPostLikeEntity>createList("postLikeEntities", PostLikeEntity.class, com.longing.longing.like.infrastructure.QPostLikeEntity.class, PathInits.DIRECT2);

    public final ListPath<PostReportEntity, com.longing.longing.report.infrastructure.QPostReportEntity> postReportEntities = this.<PostReportEntity, com.longing.longing.report.infrastructure.QPostReportEntity>createList("postReportEntities", PostReportEntity.class, com.longing.longing.report.infrastructure.QPostReportEntity.class, PathInits.DIRECT2);

    public final EnumPath<Provider> provider = createEnum("provider", Provider.class);

    public final StringPath providerId = createString("providerId");

    public final EnumPath<Role> role = createEnum("role", Role.class);

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

