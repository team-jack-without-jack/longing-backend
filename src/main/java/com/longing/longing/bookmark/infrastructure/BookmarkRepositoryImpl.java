package com.longing.longing.bookmark.infrastructure;

import com.longing.longing.bookmark.service.port.BookmarkRepository;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.post.infrastructure.PostJpaRepository;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.common.persistence.UniqueInserts;
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
import org.springframework.transaction.support.TransactionOperations;
import java.util.List;
import java.util.stream.Collectors;

import static com.longing.longing.bookmark.domain.QPostBookmark.postBookmark;
import static com.longing.longing.api.like.domain.QPostLike.postLike;
import static com.longing.longing.api.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepository {
    private final BookmarkJpaRepository bookmarkJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final PostJpaRepository postJpaRepository;
    private final TransactionOperations transactionOperations;

    @Override
    public boolean exists(long userId, long postId) {
        return bookmarkJpaRepository.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * 게시글 행을 먼저 잠근 뒤 북마크를 넣는다 (#133). 잠금과 삽입은 한 트랜잭션이고, UniqueInserts가 그 트랜잭션을 실행한다.
     * 게시글 삭제(#128)·좋아요·댓글·신고처럼 게시글 행을 먼저 잠그므로, 게시글 삭제와 교착 상태가 나지 않는다.
     * 잠금 조회는 최신 커밋을 읽는다. 그래서 그사이 지워진 게시글이면 행을 넣지 않고 ResourceNotFoundException을 던진다.
     */
    @Override
    public boolean saveIfAbsent(PostBookmark postBookmark) {
        long postId = postBookmark.getPost().getId();
        UserEntity userEntity = UserEntity.fromModel(postBookmark.getUser());
        Runnable lockPostThenInsert = () -> transactionOperations.executeWithoutResult(status -> {
            PostEntity postEntity = postJpaRepository.findByIdForUpdate(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Posts", postId));
            bookmarkJpaRepository.save(PostBookmarkEntity.fromModel(postEntity, userEntity));
        });
        return UniqueInserts.insertIfAbsent(lockPostThenInsert, PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT);
    }

    @Override
    public int delete(long userId, long postId) {
        return bookmarkJpaRepository.deleteByUserIdAndPostId(userId, postId);
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
                .where(postBookmark.user.id.eq(userId)
                        .and(post.deleted.isFalse())) // 지워진 게시글에 남은 북마크는 보이지 않게 한다 (#128)
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
                .where(postBookmark.user.id.eq(userId)
                        .and(post.deleted.isFalse())) // 지워진 게시글에 남은 북마크는 보이지 않게 한다 (#128)
                .fetchOne();

        return new PageImpl<>(posts, pageable, totalCount != null ? totalCount : 0);
    }

//    @Override
//    public Optional<PostBookmark> findByPostIdAndUserId(long postId, long userId) {
//        return Optional.empty();
//    }


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
