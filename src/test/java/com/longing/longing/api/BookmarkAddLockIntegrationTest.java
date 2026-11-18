package com.longing.longing.api;

import com.longing.longing.api.post.infrastructure.PostJpaRepository;
import com.longing.longing.api.post.infrastructure.PostRepositoryImpl;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.bookmark.controller.port.BookmarkService;
import com.longing.longing.bookmark.infrastructure.BookmarkJpaRepository;
import com.longing.longing.bookmark.infrastructure.BookmarkRepositoryImpl;
import com.longing.longing.bookmark.service.BookmarkServiceImpl;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionOperations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 북마크 추가가 게시글 행을 먼저 잠그는지 확인한다 (#133).
 * 게시글 삭제(#128)는 "게시글 행 잠금 → 좋아요·북마크 DELETE" 순서다. 북마크 추가도 게시글 행을 먼저 잠가야 다음이 보장된다.
 * - 삭제의 북마크 DELETE가 테이블 전체를 스캔해도 교착 상태가 없다.
 * - 그사이 지워진 게시글에 북마크 행이 생기지 않는다.
 */
class BookmarkAddLockIntegrationTest extends MariaDbIntegrationTestSupport {

    /** 한 게시글에 이만큼 북마크를 몰면 북마크 DELETE가 전체 스캔(type = ALL)이 된다. MariaDB 10.6에서 10개부터 확인했다. */
    private static final int SKEWED_BOOKMARKS = 30;

    @Autowired JPAQueryFactory queryFactory;
    @Autowired BookmarkJpaRepository bookmarkJpaRepository;
    @Autowired TransactionOperations transactionOperations;

    private long otherPost;
    private int seededUsers;

    @BeforeEach
    void seedOtherPost() {
        otherPost = seedPost(member);
    }

    @Test
    void bookmarkWaitsBehindDeletionAndEndsNotFound() throws Exception {
        // 다른 게시글에만 북마크가 있어서 삭제의 북마크 DELETE는 post_id 인덱스를 쓴다.
        seedBookmarks(otherPost, 3);
        assertThat(bookmarkDeleteAccessType()).as("access type of the deletion's bookmark DELETE").isEqualTo("range");
        long deadlocksBefore = deadlocks();

        Race race = bookmarkWhileDeletionHoldsThePost();

        assertThat(deadlocks() - deadlocksBefore).as("deadlocks during the race").isZero();
        assertThat(race.deletionError).as("deletion").isNull();
        assertThat(race.bookmarkError).as("bookmark").isInstanceOf(ResourceNotFoundException.class);
        assertThat(postDeleted()).as("post deleted").isTrue();
        assertThat(bookmarksOf(postId)).as("bookmark rows on the deleted post").isZero();
    }

    @Test
    void bookmarkWaitsBehindDeletionEvenWhenTheBookmarkDeleteScansTheWholeTable() throws Exception {
        // 지울 게시글에 북마크가 몰려 있어서 삭제의 북마크 DELETE가 테이블 전체를 스캔한다.
        // 변경 전에는 이 DELETE가 북마크 추가가 먼저 넣은 행에서 기다려 교착 상태가 났다.
        seedBookmarks(postId, SKEWED_BOOKMARKS);
        // 다른 게시글 북마크도 하나 둔다. 테이블 전부가 아니라 대부분이 한 게시글에 몰려도 전체 스캔이 된다.
        seedBookmarks(otherPost, 1);
        assertThat(bookmarkDeleteAccessType()).as("access type of the deletion's bookmark DELETE").isEqualTo("ALL");
        long deadlocksBefore = deadlocks();

        Race race = bookmarkWhileDeletionHoldsThePost();

        assertThat(deadlocks() - deadlocksBefore).as("deadlocks during the race").isZero();
        assertThat(race.deletionError).as("deletion").isNull();
        assertThat(race.bookmarkError).as("bookmark").isInstanceOf(ResourceNotFoundException.class);
        assertThat(postDeleted()).as("post deleted").isTrue();
        assertThat(bookmarksOf(postId)).as("bookmark rows on the deleted post").isZero();
    }

    @Test
    void deletionCommittedRightAfterTheReadLeavesNoBookmark() throws Exception {
        AtomicBoolean deleted = new AtomicBoolean();
        // 북마크 추가가 게시글을 읽은 직후, 다른 요청이 그 게시글을 지우고 커밋한다.
        // 트랜잭션이 없는 inRequest 안이라 삭제는 같은 EntityManager를 쓰고, 끝나면 그 EntityManager를 clear한다.
        // 실제 요청에서는 읽은 게시글 엔티티가 남지만, 잠금 조회는 JPQL이라 어느 쪽이든 DB를 읽는다.
        PostRepository racing = afterFirstCall(PostRepository.class, postRepository, "findById", deleted,
                () -> asAnotherRequest(() -> postRepository.softDelete(postId)));
        BookmarkService service = new BookmarkServiceImpl(bookmarkRepository, userRepository, racing);

        Throwable error = catchThrowable(() -> inRequest(() -> {
            service.addBookmark(details(member), postId);
            return null;
        }));

        assertThat(deleted).as("deletion ran between the read and the save").isTrue();
        assertThat(error).as("bookmark").isInstanceOf(ResourceNotFoundException.class);
        assertThat(postDeleted()).as("post deleted").isTrue();
        assertThat(bookmarksOf(postId)).as("bookmark rows on the deleted post").isZero();
    }

    @Test
    void deletionWaitsBehindBookmarkAndRemovesIt() throws Exception {
        CountDownLatch saved = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        // 북마크를 넣은 직후, 커밋하기 전에 멈춘다. 이때 북마크 추가는 게시글 행과 새 북마크 행을 쥐고 있다.
        BookmarkJpaRepository pausing = afterFirstCall(BookmarkJpaRepository.class, bookmarkJpaRepository, "save",
                new AtomicBoolean(), () -> {
                    saved.countDown();
                    await(proceed);
                });
        BookmarkService service = new BookmarkServiceImpl(
                new BookmarkRepositoryImpl(pausing, queryFactory, postJpaRepository, transactionOperations),
                userRepository, postRepository);
        long deadlocksBefore = deadlocks();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Throwable bookmarkError;
        Throwable deletionError;
        try {
            Future<?> bookmark = pool.submit(() -> inRequest(() -> {
                service.addBookmark(details(member), postId);
                return null;
            }));
            assertThat(saved.await(10, TimeUnit.SECONDS)).as("bookmark saved but not committed").isTrue();
            Future<?> deletion = pool.submit(() -> inTransaction(() -> postRepository.softDelete(postId)));
            awaitLockWait("posts");   // 삭제가 "select … from posts … for update"에서 기다린다.
            proceed.countDown();
            bookmarkError = errorOf(bookmark);
            deletionError = errorOf(deletion);
        } finally {
            proceed.countDown();
            shutdown(pool);
        }

        assertThat(deadlocks() - deadlocksBefore).as("deadlocks during the race").isZero();
        assertThat(bookmarkError).as("bookmark").isNull();
        assertThat(deletionError).as("deletion").isNull();
        assertThat(postDeleted()).as("post deleted").isTrue();
        assertThat(bookmarksOf(postId)).as("the new bookmark is removed with the post").isZero();
    }

    /**
     * 삭제(PostRepositoryImpl.softDelete)가 게시글 행을 잠근 직후 멈춘 사이에 member가 북마크를 추가한다.
     * 북마크 추가가 잠금을 기다리기 시작하면 삭제를 이어서 실행하고 커밋한다.
     */
    private Race bookmarkWhileDeletionHoldsThePost() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        PostJpaRepository pausing = afterFirstCall(PostJpaRepository.class, postJpaRepository, "findByIdForUpdate",
                new AtomicBoolean(), () -> {
                    locked.countDown();
                    await(proceed);
                });
        // 직접 만든 저장소에는 @Transactional 프록시가 없어서 inTransaction 안에서 부른다.
        PostRepository deleting = new PostRepositoryImpl(pausing, queryFactory);
        BookmarkService service = new BookmarkServiceImpl(bookmarkRepository, userRepository, postRepository);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Race race = new Race();
        try {
            Future<?> deletion = pool.submit(() -> inTransaction(() -> deleting.softDelete(postId)));
            assertThat(locked.await(10, TimeUnit.SECONDS)).as("deletion locked the post").isTrue();
            Future<?> bookmark = pool.submit(() -> inRequest(() -> {
                service.addBookmark(details(member), postId);
                return null;
            }));
            // 북마크 추가는 saveIfAbsent의 "select … from posts … for update"에서 기다린다.
            // 변경 전처럼 외래 키 확인("insert into post_bookmarks …")에서 기다리면 여기서 시간 초과로 실패한다.
            awaitLockWait("posts");
            proceed.countDown();
            race.deletionError = errorOf(deletion);
            race.bookmarkError = errorOf(bookmark);
        } finally {
            proceed.countDown();
            shutdown(pool);
        }
        return race;
    }

    private static final class Race {
        Throwable deletionError;
        Throwable bookmarkError;
    }

    /** 새 사용자 count명이 targetPost를 북마크한 상태를 만든다. (사용자, 게시글) unique 때문에 사용자를 새로 만든다. */
    private void seedBookmarks(long targetPost, int count) {
        for (int i = 0; i < count; i++) {
            seededUsers++;
            User user = saveUser("b" + seededUsers + "@example.com", "b-" + seededUsers);
            jdbc.update("INSERT INTO post_bookmarks (post_id, user_id, deleted) VALUES (?, ?, 0)", targetPost, user.getId());
        }
    }

    /**
     * 삭제(#128)의 북마크 DELETE(PostJpaRepository.deleteBookmarksOf)가 쓰는 접근 방식. 통계를 새로 만든 뒤 본다.
     * ALL이면 테이블 전체 스캔이다. 그 DELETE가 바뀌면 여기 SQL도 맞춘다.
     */
    private String bookmarkDeleteAccessType() {
        jdbc.execute("ANALYZE TABLE post_bookmarks");
        return jdbc.queryForObject("EXPLAIN DELETE FROM post_bookmarks WHERE post_id = " + postId,
                (rows, rowNum) -> rows.getString("type"));
    }

    /**
     * 서버가 시작된 뒤 InnoDB가 감지한 교착 상태 수. 테스트는 차이만 본다.
     * 교착 상태로 롤백된 북마크 추가는 UniqueInserts가 한 번 다시 실행하므로 오류 없이 끝날 수 있다.
     * 그래서 오류 대신 이 수로 확인한다. 테스트 클래스가 한 컨테이너에서 차례로 실행된다고 가정한다.
     */
    private long deadlocks() {
        return jdbc.queryForObject("SHOW GLOBAL STATUS LIKE 'Innodb_deadlocks'", (rows, rowNum) -> rows.getLong(2));
    }

    private long bookmarksOf(long targetPost) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post_bookmarks WHERE post_id = ?", Long.class, targetPost);
    }

    private static CustomUserDetails details(User user) {
        return new CustomUserDetails(user.getEmail(), user.getProvider());
    }

    private static Throwable errorOf(Future<?> future) throws Exception {
        try {
            future.get(30, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch was not released in 30s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void shutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        // 멈춘 스레드가 커넥션을 쥔 채 다음 테스트의 TRUNCATE와 겹치지 않게 끝날 때까지 기다린다.
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("BookmarkAddLockIntegrationTest: worker threads still running after 10s");
        }
    }
}
