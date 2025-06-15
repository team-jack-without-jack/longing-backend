package com.longing.longing.api.user.domain;

import com.longing.longing.api.user.infrastructure.UserEntity;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

import javax.annotation.processing.Generated;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QUser extends EntityPathBase<UserEntity> {

    private static final long serialVersionUID = 1L;

    public static final QUser user = new QUser("user");

    public final NumberPath<Long> id = createNumber("id", Long.class);
    public final StringPath name = createString("name");
    public final StringPath email = createString("email");

    public QUser(String variable) {
        super(UserEntity.class, forVariable(variable));
    }

    public QUser(Path<? extends UserEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(UserEntity.class, metadata);
    }
}