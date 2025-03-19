package com.longing.longing.post.infrastructure;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostEntity is a Querydsl query type for PostEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostEntity extends EntityPathBase<PostEntity> {

    private static final long serialVersionUID = 2114189373L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostEntity postEntity = new QPostEntity("postEntity");

    public final com.longing.longing.common.QBaseTimeEntity _super = new com.longing.longing.common.QBaseTimeEntity(this);

    public final NumberPath<Integer> commentCount = createNumber("commentCount", Integer.class);

    public final ListPath<com.longing.longing.comment.infrastructure.CommentEntity, com.longing.longing.comment.infrastructure.QCommentEntity> commentEntities = this.<com.longing.longing.comment.infrastructure.CommentEntity, com.longing.longing.comment.infrastructure.QCommentEntity>createList("commentEntities", com.longing.longing.comment.infrastructure.CommentEntity.class, com.longing.longing.comment.infrastructure.QCommentEntity.class, PathInits.DIRECT2);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    //inherited
    public final BooleanPath deleted = _super.deleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedDate = _super.deletedDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> likeCount = createNumber("likeCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final ListPath<com.longing.longing.bookmark.infrastructure.PostBookmarkEntity, com.longing.longing.bookmark.infrastructure.QPostBookmarkEntity> postBookmarkEntities = this.<com.longing.longing.bookmark.infrastructure.PostBookmarkEntity, com.longing.longing.bookmark.infrastructure.QPostBookmarkEntity>createList("postBookmarkEntities", com.longing.longing.bookmark.infrastructure.PostBookmarkEntity.class, com.longing.longing.bookmark.infrastructure.QPostBookmarkEntity.class, PathInits.DIRECT2);

    public final ListPath<com.longing.longing.common.infrastructure.PostImageEntity, com.longing.longing.common.infrastructure.QPostImageEntity> postImageEntities = this.<com.longing.longing.common.infrastructure.PostImageEntity, com.longing.longing.common.infrastructure.QPostImageEntity>createList("postImageEntities", com.longing.longing.common.infrastructure.PostImageEntity.class, com.longing.longing.common.infrastructure.QPostImageEntity.class, PathInits.DIRECT2);

    public final ListPath<com.longing.longing.like.infrastructure.PostLikeEntity, com.longing.longing.like.infrastructure.QPostLikeEntity> postLikeEntities = this.<com.longing.longing.like.infrastructure.PostLikeEntity, com.longing.longing.like.infrastructure.QPostLikeEntity>createList("postLikeEntities", com.longing.longing.like.infrastructure.PostLikeEntity.class, com.longing.longing.like.infrastructure.QPostLikeEntity.class, PathInits.DIRECT2);

    public final StringPath title = createString("title");

    public final com.longing.longing.user.infrastructure.QUserEntity user;

    public QPostEntity(String variable) {
        this(PostEntity.class, forVariable(variable), INITS);
    }

    public QPostEntity(Path<? extends PostEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostEntity(PathMetadata metadata, PathInits inits) {
        this(PostEntity.class, metadata, inits);
    }

    public QPostEntity(Class<? extends PostEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.longing.longing.user.infrastructure.QUserEntity(forProperty("user")) : null;
    }

}

