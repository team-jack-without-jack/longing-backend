package com.longing.longing.api.bookmark.infrastructure;

import com.longing.longing.api.bookmark.domain.PostBookmark;
import com.longing.longing.api.bookmark.domain.QPostBookmark;
import com.longing.longing.api.bookmark.service.port.BookmarkRepository;
import com.longing.longing.api.like.domain.QPostLike;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.QPost;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepository {
    private final BookmarkJpaRepository bookmarkJpaRepository;
    private final JPAQueryFactory queryFactory;

    public Optional<PostBookmark> findByPostAndUser(Post post, User user) {
        return bookmarkJpaRepository.findByPostIdAndUserId(post.getId(), user.getId()).map(PostBookmarkEntity::toModel);
    }

    @Override
    public void save(PostBookmark postBookmark) {
        PostEntity postEntity   = PostEntity.fromModel(postBookmark.getPost());
        UserEntity userEntity   = UserEntity.fromModel(postBookmark.getUser());
        bookmarkJpaRepository.save(PostBookmarkEntity.fromModel(postEntity, userEntity));
    }

    @Override
    public void deleteById(Long postId) {
        bookmarkJpaRepository.deleteById(postId);
    }

//    @Override
//    public Page<Post> getBookmarkPost(Long userId, Pageable pageable) {
//        // QueryDSL에서 사용할 정렬 기준 가져오기
//        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(pageable);
//
//        // 북마크한 게시글 리스트 가져오기
//        List<Post> posts = queryFactory
//                .select(post)
//                .from(postBookmark)
//                .join(postBookmark.post, post)
//                .where(postBookmark.user.id.eq(userId))
//                .orderBy(orderSpecifier) // 동적 정렬 적용
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .fetch()
//                .stream().map(PostEntity::toModel)
//                .collect(Collectors.toList());
//
//        // 전체 개수 조회
//        Long totalCount = queryFactory
//                .select(post.count())
//                .from(postBookmark)
//                .join(postBookmark.post, post)
//                .where(postBookmark.user.id.eq(userId))
//                .fetchOne();
//
//        return new PageImpl<>(posts, pageable, totalCount != null ? totalCount : 0);
//    }

    @Override
    public Page<Post> getBookmarkPost(Long userId, Pageable pageable) {
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(pageable);

        // 북마크한 게시글 리스트 가져오기 (liked, bookmarked 값 함께 조회)
        List<Post> posts = queryFactory
                .select(
                        QPost.post,
                        QPostBookmark.postBookmark.isNotNull(), // 북마크 여부
                        QPostLike.postLike.isNotNull() // 좋아요 여부
                )
                .from(QPostBookmark.postBookmark)
                .join(QPostBookmark.postBookmark.post, QPost.post)
                .leftJoin(QPostLike.postLike) // 좋아요 테이블과 조인
                .on(QPostLike.postLike.post.eq(QPost.post).and(QPostLike.postLike.user.id.eq(userId)))
                .where(QPostBookmark.postBookmark.user.id.eq(userId))
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(result -> {
                    PostEntity postEntity = result.get(0, PostEntity.class);
                    Boolean bookmarked = result.get(1, Boolean.class);
                    Boolean liked = result.get(2, Boolean.class);
                    return postEntity.toModel(bookmarked, liked);
                })
                .collect(Collectors.toList());

        // 전체 개수 조회
        Long totalCount = queryFactory
                .select(QPost.post.count())
                .from(QPostBookmark.postBookmark)
                .join(QPostBookmark.postBookmark.post, QPost.post)
                .where(QPostBookmark.postBookmark.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(posts, pageable, totalCount != null ? totalCount : 0);
    }

//    @Override
//    public Optional<PostBookmark> findByPostIdAndUserId(long postId, long userId) {
//        return Optional.empty();
//    }


    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return QPost.post.id.desc(); // 기본 정렬 (최신순)
        }

        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(null);
        if (sortOrder == null) {
            return QPost.post.id.desc();
        }

        String sortProperty = sortOrder.getProperty(); // 정렬할 필드명
        Order order = sortOrder.isAscending() ? Order.ASC : Order.DESC; // 정렬 방향

        PathBuilder<Post> entityPath = new PathBuilder<>(Post.class, "post"); // ✅ Post 타입 명시
        return new OrderSpecifier<>(order, entityPath.getString(sortProperty));
    }
}
