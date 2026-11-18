package com.longing.longing.api;

import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.controller.port.ReportService;
import com.longing.longing.api.user.controller.port.UserService;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글 삭제 경로(작성자 삭제·차단·신고 자동 삭제·회원 탈퇴)의 연관 데이터 정책과 잠금 순서 (#128).
 * 정책: 신고는 남고, 좋아요·북마크는 지워지고, 댓글·이미지·게시글은 soft delete된다.
 */
class PostDeletionIntegrationTest extends MariaDbIntegrationTestSupport {

    private User other;

    @BeforeEach
    void seedOtherUser() {
        other = saveUser("other@example.com", "other-1");
    }

    // ---- 경로별 정책 ----

    @Test
    void authorDeleteKeepsReportsAndRemovesLikesAndBookmarks() {
        seedChildren(postId);

        inTransaction(() -> {
            postService(postRepository).deletePost(postId, member);
            return null;
        });

        assertDeletedKeepingReports(postId, 1);
    }

    @Test
    void blockKeepsReportsAndRemovesLikesAndBookmarks() {
        seedChildren(postId);

        inTransaction(() -> {
            postService(postRepository).blockPost(postId);
            return null;
        });

        assertDeletedKeepingReports(postId, 1);
    }

    @Test
    void reportAutoDeleteKeepsEveryReport() {
        seedChildren(postId);   // HARASSMENT 신고 1건 포함
        ReportService service = reportService(reportRepository);

        for (int i = 1; i <= 3; i++) {
            User reporter = saveUser("r" + i + "@example.com", "r-" + i);
            inTransaction(() -> service.createPostReport(postId, report(ReportReason.ADVERTISEMENT), reporter));
        }

        assertDeletedKeepingReports(postId, 4);
    }

    @Test
    void withdrawalDeletesOnlyTheUsersPostsAndKeepsTheirReports() {
        long secondPost = seedPost(member);
        long othersPost = seedPost(other);
        seedChildren(postId);
        seedChildren(secondPost);
        seedChildren(othersPost);

        inTransaction(() -> {
            userService().deavtivateUser(member.getEmail(), member.getProvider());
            return null;
        });

        assertDeletedKeepingReports(postId, 1);
        assertDeletedKeepingReports(secondPost, 1);
        // 다른 사용자의 게시글과 그 연관 데이터는 그대로다. 탈퇴한 사용자가 그 게시글에 남긴 댓글·이미지도 남긴다(범위 밖).
        assertThat(postDeleted(othersPost)).isFalse();
        assertThat(rowsOf("post_likes", othersPost)).isEqualTo(1);
        assertThat(rowsOf("post_bookmarks", othersPost)).isEqualTo(1);
        assertThat(liveRowsOf("comments", othersPost)).isEqualTo(1);
        assertThat(liveRowsOf("post_images", othersPost)).isEqualTo(1);
        assertThat(rowsOf("post_reports", othersPost)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT deleted FROM users WHERE id = ?", Boolean.class, member.getId()))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted_date IS NOT NULL FROM users WHERE id = ?", Boolean.class, member.getId()))
                .as("user deleted_date set").isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted FROM users WHERE id = ?", Boolean.class, other.getId()))
                .as("other user still live").isFalse();
    }

    // ---- 동시 요청 ----

    @Test
    void postDeleteWaitsBehindCommentDeleteInsteadOfDeadlocking() throws Exception {
        long commentId = seedComment(postId, member, false);
        assertBothFinish("UPDATE posts SET comment_count = comment_count - 1 WHERE id = " + postId,
                "UPDATE comments SET deleted = 1, deleted_date = NOW() WHERE id = " + commentId + " AND deleted = 0",
                () -> postService(postRepository).deletePost(postId, member));
        assertThat(postDeleted()).isTrue();
        assertThat(commentDeleted(commentId)).isTrue();
    }

    @Test
    void withdrawalWaitsBehindCommentDeleteInsteadOfDeadlocking() throws Exception {
        long commentId = seedComment(postId, member, false);
        assertBothFinish("UPDATE posts SET comment_count = comment_count - 1 WHERE id = " + postId,
                "UPDATE comments SET deleted = 1, deleted_date = NOW() WHERE id = " + commentId + " AND deleted = 0",
                () -> userService().deavtivateUser(member.getEmail(), member.getProvider()));
        assertThat(postDeleted()).isTrue();
        assertThat(commentDeleted(commentId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted FROM users WHERE id = ?", Boolean.class, member.getId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted_date IS NOT NULL FROM users WHERE id = ?", Boolean.class, member.getId()))
                .as("user deleted_date set").isTrue();
    }

    @Test
    void withdrawalWaitsBehindLikeOnItsLaterPostInsteadOfDeadlocking() throws Exception {
        // 좋아요가 하나도 없어서, 앞 게시글의 좋아요 삭제가 남기는 간격 잠금이 뒤 게시글의 좋아요 자리까지 덮는다.
        long laterPost = seedPost(member);
        assertBothFinish(
                "UPDATE posts SET like_count = like_count + 1 WHERE id = " + laterPost,   // 좋아요: 게시글 행 먼저
                "INSERT INTO post_likes (post_id, user_id, deleted) VALUES (" + laterPost + ", " + other.getId() + ", 0)",
                () -> userService().deavtivateUser(member.getEmail(), member.getProvider()));

        assertThat(postDeleted(laterPost)).isTrue();
        assertThat(rowsOf("post_likes", laterPost)).as("like removed with the post").isZero();
    }

    @Test
    void concurrentDeletesOfSamePostBothEndQuietly() throws Exception {
        seedChildren(postId);
        AtomicInteger checks = new AtomicInteger();
        PostService racing = postService(pausingAfter(PostRepository.class, postRepository, "findByIdAndUserId", checks));

        List<Object> results = runConcurrently(2, () -> inTransaction(() -> {
            racing.deletePost(postId, member);
            return null;
        }));

        assertThat(checks).hasValue(2);
        assertThat(failures(results)).isEmpty();
        assertDeletedKeepingReports(postId, 1);
    }

    // ---- 저장소 반환값 ----

    @Test
    void softDeleteReturnsWhetherItDeletedAnything() {
        long secondPost = seedPost(member);

        assertThat(inTransaction(() -> postRepository.softDelete(postId))).isEqualTo(1);
        assertThat(inTransaction(() -> postRepository.softDelete(postId))).as("already deleted").isZero();
        assertThat(inTransaction(() -> postRepository.softDelete(999_999L))).as("missing").isZero();
        // 회원 탈퇴는 살아 있는 게시글만 센다.
        assertThat(inTransaction(() -> postRepository.softDeleteAllByUserId(member.getId()))).isEqualTo(1);
        assertThat(postDeleted(secondPost)).isTrue();
    }

    // ---- 북마크 목록 ----

    @Test
    void bookmarkListHidesDeletedPostEvenIfBookmarkRowRemains() {
        long livePost = seedPost(other);
        seedBookmark(postId, member);
        seedBookmark(livePost, member);
        // #133 이전에 생긴 행처럼, 지워진 게시글에 북마크 행이 남은 상황
        jdbc.update("UPDATE posts SET deleted = 1, deleted_date = NOW() WHERE id = ?", postId);

        Page<Post> page = inTransaction(() -> bookmarkRepository.getBookmarkPost(member.getId(), PageRequest.of(0, 10)));

        assertThat(page.getContent()).extracting(Post::getId).containsExactly(livePost);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    // ---- helpers ----

    private UserService userService() {
        return UserServiceImpl.builder()
                .userRepository(userRepository)
                .postRepository(postRepository)
                .build();
    }

    /**
     * holder가 firstWrite로 게시글 행을 잠근 채 있을 때 삭제가 게시글 행을 기다리게 하고, 그 뒤 holder가 secondWrite를 실행하고 커밋한다.
     * 둘 다 교착 상태 없이 끝나야 한다.
     */
    private void assertBothFinish(String firstWrite, String secondWrite, Runnable deletion) throws Exception {
        ExecutorService deleter = Executors.newSingleThreadExecutor();
        Throwable holderError = null;
        Throwable deletionError = null;
        try (Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            // 아직 커밋하지 않은 쓰기: 다른 쓰기 경로(댓글 삭제 #126, 좋아요 등)처럼 게시글 행을 먼저 잠근다.
            try (Statement statement = holder.createStatement()) {
                statement.executeUpdate(firstWrite);
            }
            Future<?> deleting = deleter.submit(() -> inTransaction(() -> {
                deletion.run();
                return null;
            }));
            // 삭제가 게시글 행을 기다릴 때까지 기다린다.
            // 변경 전: cascade가 댓글 행을 먼저 잡은 뒤 "UPDATE posts …"에서 기다린다.
            // 변경 후: 아무것도 잡지 않은 채 "SELECT … FROM posts WHERE id = … FOR UPDATE"에서 기다린다.
            //         (회원 탈퇴는 잠그지 않고 id를 읽은 뒤 한 건씩 잠근다.)
            awaitLockWait("posts");
            // holder의 두 번째 문장(예: 댓글 삭제의 댓글 soft delete, 좋아요의 행 추가)을 실행하고 커밋한다.
            try (Statement statement = holder.createStatement()) {
                statement.executeUpdate(secondWrite);
                holder.commit();
            } catch (SQLException e) {
                holderError = e;
                holder.rollback();
            }
            try {
                deleting.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                deletionError = e.getCause();
            }
        } finally {
            deleter.shutdownNow();
            // 멈춘 스레드가 커넥션을 쥔 채 다음 테스트의 TRUNCATE와 겹치지 않게 끝날 때까지 기다린다.
            // finally에서 단언하면 원래 실패를 가리므로 기록만 한다.
            if (!deleter.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("assertBothFinish: deleter thread still running after 10s");
            }
        }

        // 교착 상태가 나면 둘 중 하나가 1213으로 실패한다.
        assertThat(holderError).as("holder").isNull();
        assertThat(deletionError).as("deletion").isNull();
    }

    /** 댓글 1, 이미지 1, 좋아요 1, 북마크 1, 신고 1(HARASSMENT)을 붙인다. 좋아요·북마크·신고는 other가 한다. */
    private void seedChildren(long targetPostId) {
        seedComment(targetPostId, member, false);
        jdbc.update("INSERT INTO post_images (address, post_id, user_id, deleted) VALUES (?, ?, ?, 0)",
                "img-" + targetPostId, targetPostId, member.getId());
        jdbc.update("INSERT INTO post_likes (post_id, user_id, deleted) VALUES (?, ?, 0)", targetPostId, other.getId());
        seedBookmark(targetPostId, other);
        jdbc.update("INSERT INTO post_reports (post_id, reporter_id, report_reason) VALUES (?, ?, ?)",
                targetPostId, other.getId(), ReportReason.HARASSMENT.name());
    }

    private void seedBookmark(long targetPostId, User user) {
        jdbc.update("INSERT INTO post_bookmarks (post_id, user_id, deleted) VALUES (?, ?, 0)", targetPostId, user.getId());
    }

    /** 게시글은 soft delete, 신고는 그대로, 좋아요·북마크는 0, 댓글·이미지는 행은 남고 모두 soft delete. */
    private void assertDeletedKeepingReports(long targetPostId, long reports) {
        assertThat(postDeleted(targetPostId)).as("post deleted").isTrue();
        assertThat(rowsOf("post_reports", targetPostId)).as("reports kept").isEqualTo(reports);
        assertThat(rowsOf("post_likes", targetPostId)).as("likes removed").isZero();
        assertThat(rowsOf("post_bookmarks", targetPostId)).as("bookmarks removed").isZero();
        assertThat(rowsOf("comments", targetPostId)).as("comment rows kept").isEqualTo(1);
        assertThat(liveRowsOf("comments", targetPostId)).as("comments soft deleted").isZero();
        assertThat(rowsOf("post_images", targetPostId)).as("image rows kept").isEqualTo(1);
        assertThat(liveRowsOf("post_images", targetPostId)).as("images soft deleted").isZero();
        assertThat(jdbc.queryForObject("SELECT deleted_date IS NOT NULL FROM posts WHERE id = ?", Boolean.class, targetPostId))
                .as("post deleted_date set").isTrue();
        assertThat(rowsWithoutDeletedDate("comments", targetPostId)).as("comments deleted_date set").isZero();
        assertThat(rowsWithoutDeletedDate("post_images", targetPostId)).as("images deleted_date set").isZero();
    }

    private boolean postDeleted(long targetPostId) {
        return jdbc.queryForObject("SELECT deleted FROM posts WHERE id = ?", Boolean.class, targetPostId);
    }

    private long rowsOf(String table, long targetPostId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE post_id = ?", Long.class, targetPostId);
    }

    private long rowsWithoutDeletedDate(String table, long targetPostId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE post_id = ? AND deleted_date IS NULL",
                Long.class, targetPostId);
    }

    private long liveRowsOf(String table, long targetPostId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE post_id = ? AND deleted = 0",
                Long.class, targetPostId);
    }
}
