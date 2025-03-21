package com.longing.longing.bookmark.infrastructure;

import com.longing.longing.bookmark.service.port.BookmarkRepository;
import com.longing.longing.post.domain.Post;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.domain.User;
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

import static com.longing.longing.bookmark.domain.QPostBookmark.postBookmark;
import static com.longing.longing.like.domain.QPostLike.postLike;
import static com.longing.longing.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepository {
    private final BookmarkJpaRepository bookmarkJpaRepository;
    private final JPAQueryFactory queryFactory;

    public Optional<PostBookmark> findByPostAndUser(Post post, User user) {
        return bookmarkJpaRepository.findByPostIdAndUserId(post.getId(), user.getId()).map(PostBookmarkEntity::toModel);
    }

    @Override
    public void save(PostBookmarkEntity postBookmarkEntity) {
        bookmarkJpaRepository.save(postBookmarkEntity);
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
                        post,
                        postBookmark.isNotNull(), // 북마크 여부
                        postLike.isNotNull() // 좋아요 여부
                )
                .from(postBookmark)
                .join(postBookmark.post, post)
                .leftJoin(postLike) // 좋아요 테이블과 조인
                .on(postLike.post.eq(post).and(postLike.user.id.eq(userId)))
                .where(postBookmark.user.id.eq(userId))
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
                .select(post.count())
                .from(postBookmark)
                .join(postBookmark.post, post)
                .where(postBookmark.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(posts, pageable, totalCount != null ? totalCount : 0);
    }


    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return post.id.desc(); // 기본 정렬 (최신순)
        }

        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(null);
        if (sortOrder == null) {
            return post.id.desc();
        }

        String sortProperty = sortOrder.getProperty(); // 정렬할 필드명
        Order order = sortOrder.isAscending() ? Order.ASC : Order.DESC; // 정렬 방향

        PathBuilder<Post> entityPath = new PathBuilder<>(Post.class, "post"); // ✅ Post 타입 명시
        return new OrderSpecifier<>(order, entityPath.getString(sortProperty));
    }
}
