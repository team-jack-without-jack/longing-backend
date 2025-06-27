package com.longing.longing.api.post.infrastructure;

import com.longing.longing.api.like.infrastructure.QPostLikeEntity;
import com.longing.longing.bookmark.infrastructure.QPostBookmarkEntity;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.port.PostRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.longing.longing.bookmark.domain.QPostBookmark.postBookmark;
import static com.longing.longing.api.like.domain.QPostLike.postLike;
import static com.longing.longing.api.post.domain.QPost.post;


@Slf4j
@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;
    private final JPAQueryFactory queryFactory;

    public Optional<Post> findById(Long id, Long userId) {
//        return postJpaRepository.findById(id).map(PostEntity::toModel);
        return postJpaRepository.findById(id).map(postEntity -> {
            // 해당 게시물이 사용자가 좋아요 또는 북마크했는지 확인
            Boolean bookmarked = queryFactory
                    .select(postBookmark.isNotNull())
                    .from(postBookmark)
                    .where(postBookmark.post.id.eq(id)
                            .and(postBookmark.user.id.eq(userId)))
                    .fetchOne();

            Boolean liked = queryFactory
                    .select(postLike.isNotNull())
                    .from(postLike)
                    .where(postLike.post.id.eq(id)
                            .and(postLike.user.id.eq(userId)))
                    .fetchOne();

            return postEntity.toModel(bookmarked != null && bookmarked, liked != null && liked);
        });
    }


    @Override
    public Post save(Post post) {
        return postJpaRepository.save(PostEntity.fromModel(post)).toModel();
    }

    @Override
    public Page<Post> findAll(Long userId, Pageable pageable) {
        PathBuilder<PostEntity> postPath = new PathBuilder<>(PostEntity.class, "post");
        // 정렬 조건 만들기
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;

            // 필드 타입에 맞게 처리 (필요시 추가 가능)
            switch (property) {
                case "id":
                    orderSpecifiers.add(new OrderSpecifier<>(direction, postPath.getNumber(property, Long.class)));
                case "title":
                case "content":
                    orderSpecifiers.add(new OrderSpecifier<>(direction, postPath.getString(property)));
                    break;
                case "likeCount":
                case "commentCount":
                    orderSpecifiers.add(new OrderSpecifier<>(direction, postPath.getNumber(property, Integer.class)));
                    break;
                case "createdDate":
                case "modifiedDate":
                    orderSpecifiers.add(new OrderSpecifier<>(direction, postPath.getDateTime(property, LocalDateTime.class)));
                    break;
                default:
                    throw new IllegalArgumentException("cannot sorted by: " + property);
            }
        }

        // 쿼리 실행
        List<Tuple> results = queryFactory
                .select(
                        post,
                        JPAExpressions.selectOne()
                                .from(postBookmark)
                                .where(postBookmark.post.eq(post)
                                        .and(postBookmark.user.id.eq(userId)))
                                .exists(),
                        JPAExpressions.selectOne()
                                .from(postLike)
                                .where(postLike.post.eq(post)
                                        .and(postLike.user.id.eq(userId)))
                                .exists()
                )
                .from(post)
                .where(post.deleted.eq(false))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<Post> posts = results.stream()
                .map(tuple -> {
                    PostEntity postEntity = tuple.get(post);
                    Boolean bookmarked = tuple.get(1, Boolean.class);
                    Boolean liked = tuple.get(2, Boolean.class);
                    return postEntity.toModel(bookmarked, liked);
                })
                .collect(Collectors.toList());

        long total = queryFactory
                .select(post.count())
                .from(post)
                .where(post.deleted.eq(false))
                .fetchOne();

        return new PageImpl<>(posts, pageable, total);
    }



    public void deleteById(Long postId) {
        postJpaRepository.deleteById(postId);
    }

    @Override
    public Page<Post> findAllwithLikeCountAndSearch(Long userId, String keyword, Pageable pageable) {
        QPostEntity post = QPostEntity.postEntity;
        QPostLikeEntity like = QPostLikeEntity.postLikeEntity;
        QPostBookmarkEntity bookmark = QPostBookmarkEntity.postBookmarkEntity;

        BooleanExpression condition = post.title.containsIgnoreCase(keyword)
                .or(post.content.containsIgnoreCase(keyword));

        JPQLQuery<Tuple> query = queryFactory
                .select(post, like.isNotNull(), bookmark.isNotNull())
                .from(post)
                .leftJoin(like).on(like.post.id.eq(post.id).and(like.user.id.eq(userId)))
                .leftJoin(bookmark).on(bookmark.post.id.eq(post.id).and(bookmark.user.id.eq(userId)))
                .where(condition);

        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            PathBuilder<PostEntity> entityPath = new PathBuilder<>(PostEntity.class, "postEntity");

            // 기본적으로 StringPath로 처리 (문자열 정렬용)
            OrderSpecifier<?> orderSpecifier = order.isAscending()
                    ? entityPath.getString(property).asc()
                    : entityPath.getString(property).desc();

            query.orderBy(orderSpecifier);
        }

        List<Tuple> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<Post> content = results.stream()
                .map(tuple -> {
                    PostEntity postEntity = tuple.get(post);
                    Boolean liked = tuple.get(like.isNotNull());
                    Boolean bookmarked = tuple.get(bookmark.isNotNull());
                    return postEntity.toModel(bookmarked, liked);
                })
                .collect(Collectors.toList());

        long total = queryFactory
                .select(post.count())
                .from(post)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

//    @Override
//    public Page<Post> findMyPostsWithLikeCountAndSearch(Long userId, String keyword, Pageable pageable) {
//        List<Post> postEntities = postJpaRepository.findMyPostsWithLikeCountAndSearch(userId, keyword, pageable)
//                .stream()
//                .map(PostEntity::toModel)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(postEntities, pageable, postEntities.size());
//    }

//    @Override
//    public Page<Post> findMyPostsWithLikeCountAndSearch(Long userId, String keyword, Pageable pageable) {
//        Page<Object[]> results = postJpaRepository.findMyPostsWithLikeCountAndSearch(userId, keyword, pageable);
//
//        List<Post> postEntities = results.getContent().stream().map(result -> {
//            if (result.length < 3) {
//                throw new IllegalStateException("Unexpected query result format: expected at least 3 elements but got " + result.length);
//            }
//
//            // 안전한 타입 변환
//            PostEntity postEntity = (result[0] instanceof PostEntity) ? (PostEntity) result[0] : null;
//            Boolean bookmarked = (result[1] instanceof Boolean) ? (Boolean) result[1] : false;
//            Boolean liked = (result[2] instanceof Boolean) ? (Boolean) result[2] : false;
//
//            if (postEntity == null) {
//                throw new IllegalStateException("PostEntity is null in query result");
//            }
//
//            return postEntity.toModel(bookmarked, liked); // PostEntity를 Post로 변환
//        }).collect(Collectors.toList());
//
//        return new PageImpl<>(postEntities, pageable, results.getTotalElements());
//    }


    @Override
    public Page<Post> findMyPostsWithLikeCountAndSearch(Long userId, String keyword, Pageable pageable) {
//        Page<Object[]> results = postJpaRepository.findMyPostsWithLikeCountAndSearch(userId, keyword, pageable);
//
//        // Object[]를 안전하게 Post로 변환
//        List<Post> postEntities = results.getContent().stream().map(result -> {
//            if (result.length < 3) {
//                throw new IllegalStateException("Query result is invalid: " + Arrays.toString(result));
//            }
//
//            PostEntity postEntity = (result[0] instanceof PostEntity) ? (PostEntity) result[0] : null;
//            Boolean bookmarked = (result[1] instanceof Boolean) ? (Boolean) result[1] : false;
//            Boolean liked = (result[2] instanceof Boolean) ? (Boolean) result[2] : false;
//
//            if (postEntity == null) {
//                throw new IllegalStateException("PostEntity is null in query result");
//            }
//
//            return postEntity.toModel(bookmarked, liked);
//        }).collect(Collectors.toList());
//
//        return new PageImpl<>(postEntities, pageable, results.getTotalElements());

        QPostEntity post = QPostEntity.postEntity;
        QPostBookmarkEntity bookmark = QPostBookmarkEntity.postBookmarkEntity;
        QPostLikeEntity like = QPostLikeEntity.postLikeEntity;

        // --- content 쿼리 ---
        List<Tuple> results = queryFactory
                .select(
                        post,
                        JPAExpressions.selectOne()
                                .from(bookmark)
                                .where(bookmark.post.id.eq(post.id)
                                        .and(bookmark.user.id.eq(userId)))
                                .exists(),
                        JPAExpressions.selectOne()
                                .from(like)
                                .where(like.post.id.eq(post.id)
                                        .and(like.user.id.eq(userId)))
                                .exists()
                )
                .from(post)
                .where(
                        post.user.id.eq(userId),
                        post.title.containsIgnoreCase(keyword)
                                .or(post.content.containsIgnoreCase(keyword))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(post.createdDate.desc()) // 또는 post.id.desc() 등
                .fetch();

        // --- count 쿼리 ---
        long total = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.user.id.eq(userId),
                        post.title.containsIgnoreCase(keyword)
                                .or(post.content.containsIgnoreCase(keyword))
                )
                .fetchOne();

        // --- Tuple -> Post 변환 ---
        List<Post> posts = results.stream().map(tuple -> {
            PostEntity postEntity = tuple.get(post);
            Boolean bookmarked = tuple.get(1, Boolean.class);
            Boolean liked = tuple.get(2, Boolean.class);
            return postEntity.toModel(bookmarked, liked); // ← 이 메서드가 Post 객체 반환한다고 가정
        }).collect(Collectors.toList());

        return new PageImpl<>(posts, pageable, total);

    }

    @Override
    public void incrementLikeCount(Long postId) {
        postJpaRepository.incrementLikeCount(postId);
    }

    @Override
    public void decrementLikeCount(Long postId) {
        postJpaRepository.decrementLikeCount(postId);
    }

    @Override
    public void incrementCommentCount(Long postId) {
        postJpaRepository.incrementCommentCount(postId);
    }

    @Override
    public void decrementCommentCount(Long postId) {
        postJpaRepository.decrementCommentCount(postId);
    }

    @Override
    public void flush() {
        postJpaRepository.flush();
    }



}
