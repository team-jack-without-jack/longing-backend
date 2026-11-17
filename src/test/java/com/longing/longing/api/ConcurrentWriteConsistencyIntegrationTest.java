package com.longing.longing.api;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentUpdate;
import com.longing.longing.api.comment.service.port.CommentRepository;
import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostUpdate;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.controller.port.ReportService;
import com.longing.longing.api.report.infrastructure.PostReportEntity;
import com.longing.longing.api.report.service.port.ReportRepository;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.exceptions.AlreadyReportedPostException;
import com.longing.longing.common.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockMultipartFile;

import javax.persistence.EntityManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ConcurrentWriteConsistencyIntegrationTest extends MariaDbIntegrationTestSupport {

    // ---- 신고 ----

    @Test
    void concurrentReportsFromSameUserSaveOne() throws Exception {
        AtomicInteger checks = new AtomicInteger();
        ReportService racing = reportService(pausingAfter(ReportRepository.class, reportRepository, "exists", checks));

        List<Object> results = runConcurrently(THREADS, () -> inTransaction(() ->
                racing.createPostReport(postId, report(ReportReason.HARASSMENT), member)));

        // 모든 요청이 확인 단계를 지났다(경쟁이 실제로 일어났다).
        assertThat(checks).hasValue(THREADS);
        assertThat(failures(results)).hasSize(THREADS - 1)
                .allSatisfy(e -> assertThat(e).isInstanceOf(AlreadyReportedPostException.class));
        assertThat(reportRows(member.getId(), ReportReason.HARASSMENT)).isEqualTo(1);
        // 한 사람의 연타로는 자동 삭제 기준(같은 사유 3건)을 채울 수 없다.
        assertThat(postDeleted()).isFalse();
    }

    @Test
    void concurrentReportsFromThreeUsersDeleteThePost() throws Exception {
        List<User> reporters = Arrays.asList(member,
                saveUser("r2@example.com", "r-2"), saveUser("r3@example.com", "r-3"));
        AtomicInteger next = new AtomicInteger();
        AtomicInteger checks = new AtomicInteger();
        ReportService racing = reportService(pausingAfter(ReportRepository.class, reportRepository, "exists", checks));

        List<Object> results = runConcurrently(reporters.size(), () -> inTransaction(() ->
                racing.createPostReport(postId, report(ReportReason.HARASSMENT), reporters.get(next.getAndIncrement()))));

        assertThat(checks).hasValue(reporters.size());
        assertThat(failures(results)).isEmpty();
        // 자동 삭제 뒤에도 신고 행은 근거로 남는다 (#128).
        assertThat(count("post_reports")).isEqualTo((long) reporters.size());
        assertThat(postDeleted()).isTrue();
    }

    @Test
    void reportReasonsAreCountedSeparatelyAndDeletedPostCannotBeReported() {
        User second = saveUser("r2@example.com", "r-2");
        User third = saveUser("r3@example.com", "r-3");
        ReportService service = reportService(reportRepository);

        inTransaction(() -> service.createPostReport(postId, report(ReportReason.HARASSMENT), member));
        inTransaction(() -> service.createPostReport(postId, report(ReportReason.HARASSMENT), second));
        inTransaction(() -> service.createPostReport(postId, report(ReportReason.ADVERTISEMENT), third));
        assertThat(postDeleted()).isFalse();

        inTransaction(() -> service.createPostReport(postId, report(ReportReason.HARASSMENT), third));
        assertThat(postDeleted()).isTrue();

        assertThatThrownBy(() -> inTransaction(() ->
                service.createPostReport(postId, report(ReportReason.OTHERS), member)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- 스키마 ----

    @Test
    void hibernateSchemaHasTheReportUniqueIndex() {
        assertThat(uniqueIndexColumns("post_reports", PostReportEntity.REPORTER_POST_REASON_UNIQUE_CONSTRAINT))
                .containsExactly("reporter_id", "post_id", "report_reason");
    }

    @Test
    void reportMigrationCleansDuplicatesAddsConstraintAndRollsBack() throws Exception {
        String forward = script("scripts/sql/126-post-report-unique.sql");
        String rollback = script("scripts/sql/126-post-report-unique-rollback.sql");
        assertThat(forward).contains("ALTER TABLE post_reports");
        assertThat(rollback).contains("ALTER TABLE post_reports");
        long m = member.getId();
        long o = saveUser("other@example.com", "other-1").getId();
        long secondPostId = seedPost(member);

        // 같은 스크립트를 이전 스키마 모양(외래 키 포함, unique 없음)의 별도 테이블에 적용한다.
        dropMigrationTables();
        try {
            jdbc.execute("CREATE TABLE mig_post_reports (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "report_reason VARCHAR(255) NOT NULL, post_id BIGINT, reporter_id BIGINT, "
                    + "CONSTRAINT fk_mig_pr_post FOREIGN KEY (post_id) REFERENCES posts (id), "
                    + "CONSTRAINT fk_mig_pr_reporter FOREIGN KEY (reporter_id) REFERENCES users (id))");
            jdbc.update("INSERT INTO mig_post_reports (id, reporter_id, post_id, report_reason) VALUES "
                            + "(1, ?, ?, 'HARASSMENT'), (2, ?, ?, 'HARASSMENT'), (3, ?, ?, 'HARASSMENT'), "
                            + "(4, ?, ?, 'ADVERTISEMENT'), (5, ?, ?, 'HARASSMENT'), (6, ?, ?, 'HARASSMENT'), "
                            + "(7, ?, ?, 'HARASSMENT')",
                    m, postId, m, postId, m, postId, m, postId, o, postId, o, postId, m, secondPostId);

            runScript(onMigrationTables(forward));

            // (신고자, 게시글, 사유)마다 가장 오래된 행만 남고, 지운 행은 백업 테이블에 있다.
            assertThat(ids("mig_post_reports")).containsExactly(1L, 4L, 5L, 7L);
            assertThat(ids("backup_126_post_reports")).containsExactly(2L, 3L, 6L);
            // 드리프트 방지: 운영 스크립트가 만든 인덱스가 엔티티(Hibernate)가 만든 것과 같아야 한다.
            assertThat(uniqueIndexColumns("mig_post_reports", PostReportEntity.REPORTER_POST_REASON_UNIQUE_CONSTRAINT))
                    .isEqualTo(uniqueIndexColumns("post_reports", PostReportEntity.REPORTER_POST_REASON_UNIQUE_CONSTRAINT))
                    .containsExactly("reporter_id", "post_id", "report_reason");
            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO mig_post_reports (reporter_id, post_id, report_reason) VALUES (?, ?, 'HARASSMENT')",
                    m, postId))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining(PostReportEntity.REPORTER_POST_REASON_UNIQUE_CONSTRAINT);

            // 다시 실행해도 실패하지 않고 백업이 늘지 않는다.
            runScript(onMigrationTables(forward));
            assertThat(ids("backup_126_post_reports")).containsExactly(2L, 3L, 6L);

            runScript(onMigrationTables(rollback));

            assertThat(uniqueIndexColumns("mig_post_reports", PostReportEntity.REPORTER_POST_REASON_UNIQUE_CONSTRAINT))
                    .isEmpty();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS "
                    + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME IN ('fk_mig_pr_post', 'fk_mig_pr_reporter')",
                    Long.class)).isEqualTo(2L);

            // 제약이 없는 동안 생긴 중복도 적용 스크립트를 다시 실행하면 백업·정리되고 제약이 다시 생긴다.
            jdbc.update("INSERT INTO mig_post_reports (id, reporter_id, post_id, report_reason) VALUES (8, ?, ?, 'HARASSMENT')",
                    m, postId);

            runScript(onMigrationTables(forward));

            assertThat(ids("mig_post_reports")).containsExactly(1L, 4L, 5L, 7L);
            assertThat(ids("backup_126_post_reports")).containsExactly(2L, 3L, 6L, 8L);
            assertThat(uniqueIndexColumns("mig_post_reports", PostReportEntity.REPORTER_POST_REASON_UNIQUE_CONSTRAINT))
                    .containsExactly("reporter_id", "post_id", "report_reason");
        } finally {
            dropMigrationTables();
        }
    }

    @Test
    void commentCountRecountFixesDriftedCounts() throws Exception {
        long drifted = postId;
        long stale = seedPost(member);
        long correct = seedPost(member);
        seedComment(drifted, member, false);
        seedComment(drifted, member, false);
        seedComment(drifted, member, true);
        seedComment(correct, member, false);
        // 이중 감소 등으로 틀어진 값
        jdbc.update("UPDATE posts SET comment_count = ? WHERE id = ?", -2, drifted);
        jdbc.update("UPDATE posts SET comment_count = ? WHERE id = ?", 5, stale);
        long deletedPost = seedPost(member);
        seedComment(deletedPost, member, false);
        // 삭제된 게시글은 cascade로 댓글이 soft delete돼도 수가 그대로라 늘 어긋나 보인다. 재계산 대상이 아니다.
        jdbc.update("UPDATE posts SET deleted = 1, comment_count = 7 WHERE id = ?", deletedPost);

        String recount = script("scripts/sql/126-comment-count-recount.sql");
        runScript(recount);

        assertThat(commentCountOf(drifted)).isEqualTo(2);
        assertThat(commentCountOf(stale)).isZero();
        assertThat(commentCountOf(correct)).isEqualTo(1);
        assertThat(commentCountOf(deletedPost)).isEqualTo(7);

        // 다시 실행해도 그대로다.
        runScript(recount);
        assertThat(commentCountOf(drifted)).isEqualTo(2);
        assertThat(commentCountOf(correct)).isEqualTo(1);
        assertThat(commentCountOf(stale)).isZero();
    }

    @Test
    void recountWaitsForInFlightCommentWriterAndKeepsCommentsCommittedMeanwhile() throws Exception {
        long drifted = postId;
        jdbc.update("UPDATE posts SET comment_count = 5 WHERE id = ?", drifted);   // 실제 0개
        long clean = seedPost(member);
        seedComment(clean, member, false);                                          // 1개, 맞음
        String recount = script("scripts/sql/126-comment-count-recount.sql");
        ExecutorService runner = Executors.newSingleThreadExecutor();
        try (Connection writer = dataSource.getConnection()) {
            writer.setAutoCommit(false);
            // 댓글 생성과 같은 순서로, 아직 커밋하지 않은 쓰기: 게시글 댓글 수 증가(행 잠금) → 댓글 삽입
            try (Statement statement = writer.createStatement()) {
                statement.executeUpdate("UPDATE posts SET comment_count = comment_count + 1 WHERE id = " + drifted);
                statement.executeUpdate("INSERT INTO comments (content, post_id, user_id, deleted) VALUES ('w', "
                        + drifted + ", " + member.getId() + ", 0)");
            }
            Future<?> running = runner.submit(() -> runScript(recount));
            awaitLockWait("recount_126_targets");
            // 재계산이 잠금을 기다리는 동안 다른 게시글에 댓글이 커밋된다.
            seedComment(clean, member, false);
            writer.commit();
            running.get(30, TimeUnit.SECONDS);
        } finally {
            runner.shutdownNow();
        }

        assertThat(commentCountOf(drifted)).isEqualTo(1);
        assertThat(commentCountOf(clean)).isEqualTo(2);
    }

    // ---- 댓글 ----

    @Test
    void concurrentDeletesOfSameCommentDecrementOnce() throws Exception {
        long commentId = seedComment(postId, member, false);
        AtomicInteger checks = new AtomicInteger();
        CommentService racing = commentService(pausingAfter(CommentRepository.class, commentRepository, "findById", checks));

        List<Object> results = runConcurrently(THREADS, () -> inTransaction(() -> {
            racing.deleteComment(member, commentId);
            return "deleted";
        }));

        assertThat(checks).hasValue(THREADS);
        assertThat(results).containsOnlyOnce("deleted");
        assertThat(failures(results)).hasSize(THREADS - 1)
                .allSatisfy(e -> assertThat(e).isInstanceOf(ResourceNotFoundException.class));
        assertThat(commentCountOf(postId)).isZero();
        assertThat(commentDeleted(commentId)).isTrue();
    }

    @Test
    void deletingAlreadyDeletedCommentIsNotFoundAndKeepsCount() {
        long commentId = seedComment(postId, member, false);
        CommentService service = commentService(commentRepository);

        inTransaction(() -> {
            service.deleteComment(member, commentId);
            return null;
        });

        assertThatThrownBy(() -> inTransaction(() -> {
            service.deleteComment(member, commentId);
            return null;
        })).isInstanceOf(ResourceNotFoundException.class);
        assertThat(commentCountOf(postId)).isZero();
    }

    @Test
    void deleteThatLosesTheRaceRollsBackTheDecrement() throws Exception {
        long commentId = seedComment(postId, member, false);
        AtomicBoolean interleaved = new AtomicBoolean();
        // 이 요청이 댓글을 읽은 직후 다른 요청이 같은 댓글을 지운 상황
        CommentService racing = commentService(afterFirstCall(CommentRepository.class, commentRepository, "findById",
                interleaved, () -> asAnotherRequest(() ->
                        jdbc.update("UPDATE comments SET deleted = 1 WHERE id = ?", commentId))));

        assertThatThrownBy(() -> inTransaction(() -> {
            racing.deleteComment(member, commentId);
            return null;
        })).isInstanceOf(ResourceNotFoundException.class);

        assertThat(interleaved).isTrue();
        // 다른 요청의 삭제는 커밋돼 남아 있고, 이 요청이 먼저 줄였던 댓글 수는 롤백으로 되돌아갔다.
        assertThat(commentDeleted(commentId)).isTrue();
        assertThat(commentCountOf(postId)).isEqualTo(1);
    }

    @Test
    void commentUpdateDoesNotResurrectCommentDeletedMeanwhile() {
        long commentId = seedComment(postId, member, false);
        AtomicBoolean interleaved = new AtomicBoolean();
        // 수정 요청이 시작된 뒤 UPDATE 직전에, 다른 요청이 같은 댓글을 지우고 댓글 수를 줄인 상황(댓글 삭제와 같은 결과).
        // 변경 전에는 이 자리가 "댓글을 읽은 직후"였고, 수정이 엔티티 전체를 merge해 댓글을 되살렸다(reproduction.log).
        CommentService racing = commentService(beforeFirstCall(CommentRepository.class, commentRepository,
                "updateContent", interleaved, () -> asAnotherRequest(() -> {
                    jdbc.update("UPDATE comments SET deleted = 1, deleted_date = NOW() WHERE id = ?", commentId);
                    jdbc.update("UPDATE posts SET comment_count = comment_count - 1 WHERE id = ?", postId);
                })));

        Throwable thrown = catchThrowable(() -> inTransaction(() ->
                racing.updateComment(member, commentId, CommentUpdate.builder().content("edited").build())));

        assertThat(interleaved).isTrue();
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        assertThat(commentDeleted(commentId)).isTrue();
        // 되살아나거나 새 행으로 다시 생기지 않았다.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE post_id = ? AND deleted = 0",
                Long.class, postId)).isZero();
        assertThat(commentCountOf(postId)).isZero();
    }

    @Test
    void commentUpdateChangesContentAndReturnsFreshModifiedDate() {
        long commentId = seedComment(postId, member, false);
        jdbc.update("UPDATE comments SET modified_date = '2000-01-01 00:00:00' WHERE id = ?", commentId);

        Comment updated = inTransaction(() -> commentService(commentRepository)
                .updateComment(member, commentId, CommentUpdate.builder().content("edited").build()));

        assertThat(jdbc.queryForObject("SELECT content FROM comments WHERE id = ?", String.class, commentId))
                .isEqualTo("edited");
        LocalDateTime modified = jdbc.queryForObject("SELECT modified_date FROM comments WHERE id = ?",
                LocalDateTime.class, commentId);
        assertThat(modified).isAfter(LocalDateTime.of(2000, 1, 2, 0, 0));
        assertThat(updated.getContent()).isEqualTo("edited");
        assertThat(updated.getModifiedDate()).isEqualTo(modified);
    }

    @Test
    void commentUpdateWithoutContentKeepsContentAndBumpsModifiedDate() {
        long commentId = seedComment(postId, member, false);
        jdbc.update("UPDATE comments SET modified_date = '2000-01-01 00:00:00' WHERE id = ?", commentId);

        Comment updated = inTransaction(() -> commentService(commentRepository)
                .updateComment(member, commentId, CommentUpdate.builder().build()));

        assertThat(jdbc.queryForObject("SELECT content FROM comments WHERE id = ?", String.class, commentId))
                .isEqualTo("hello");
        LocalDateTime modified = jdbc.queryForObject("SELECT modified_date FROM comments WHERE id = ?",
                LocalDateTime.class, commentId);
        assertThat(modified).isAfter(LocalDateTime.of(2000, 1, 2, 0, 0));
        assertThat(updated.getContent()).isEqualTo("hello");
        assertThat(updated.getModifiedDate()).isEqualTo(modified);
    }

    @Test
    void commentDeleteArrivingDuringUpdateWaitsThenDeletesOnce() throws Exception {
        long commentId = seedComment(postId, member, false);
        AtomicBoolean interleaved = new AtomicBoolean();
        ExecutorService deleter = Executors.newSingleThreadExecutor();
        List<Future<?>> deleting = new ArrayList<>();
        try {
            // 수정의 UPDATE가 댓글 행을 잠근 직후(아직 커밋 전), 다른 요청이 같은 댓글을 지운다.
            // 삭제는 게시글 행 → 댓글 행 순서로 잠그므로 댓글 행에서 기다려야 하고, 교착 상태가 나면 안 된다.
            CommentService updating = commentService(afterFirstCall(CommentRepository.class, commentRepository,
                    "updateContent", interleaved, () -> {
                        deleting.add(deleter.submit(() -> inTransaction(() -> {
                            commentService(commentRepository).deleteComment(member, commentId);
                            return null;
                        })));
                        awaitLockWait("comments set deleted");
                    }));

            Comment updated = inTransaction(() -> updating.updateComment(member, commentId,
                    CommentUpdate.builder().content("edited").build()));
            deleting.get(0).get(30, TimeUnit.SECONDS);

            assertThat(interleaved).isTrue();
            assertThat(updated.getContent()).isEqualTo("edited");
        } finally {
            deleter.shutdownNow();
        }
        assertThat(commentDeleted(commentId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT content FROM comments WHERE id = ?", String.class, commentId))
                .isEqualTo("edited");
        assertThat(commentCountOf(postId)).isZero();
    }

    @Test
    void someoneElseCannotUpdateTheComment() {
        long commentId = seedComment(postId, member, false);
        User stranger = saveUser("stranger@example.com", "stranger-1");

        assertThatThrownBy(() -> inTransaction(() -> commentService(commentRepository)
                .updateComment(stranger, commentId, CommentUpdate.builder().content("hacked").build())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(jdbc.queryForObject("SELECT content FROM comments WHERE id = ?", String.class, commentId))
                .isEqualTo("hello");
    }

    // ---- 게시글 수정 ----

    @Test
    void updateKeepsCountersChangedWhileUpdating() throws Exception {
        jdbc.update("UPDATE posts SET modified_date = '2000-01-01 00:00:00' WHERE id = ?", postId);
        AtomicBoolean interleaved = new AtomicBoolean();
        PostRepository racingPosts = afterFirstCall(PostRepository.class, postRepository, "findById", interleaved, () ->
                // 수정 요청이 게시글을 읽은 직후, 다른 요청이 좋아요 2개·댓글 3개를 반영한 상황
                jdbc.update("UPDATE posts SET like_count = like_count + 2, comment_count = comment_count + 3 WHERE id = ?",
                        postId));
        PostService service = postService(racingPosts);

        Post updated = inRequest(() -> service.updatePost(member, postId, update("new title", "new content"),
                Collections.emptyList()));

        assertThat(interleaved).isTrue();

        assertThat(jdbc.queryForMap("SELECT title, content, like_count, comment_count FROM posts WHERE id = ?", postId))
                .containsEntry("title", "new title")
                .containsEntry("content", "new content")
                .containsEntry("like_count", 2)
                .containsEntry("comment_count", 3);
        assertThat(updated.getTitle()).isEqualTo("new title");
        assertThat(updated.getLikeCount()).isEqualTo(2);
        assertThat(updated.getCommentCount()).isEqualTo(3);
        LocalDateTime modified = jdbc.queryForObject("SELECT modified_date FROM posts WHERE id = ?", LocalDateTime.class, postId);
        assertThat(modified).isAfter(LocalDateTime.of(2000, 1, 2, 0, 0));
        assertThat(updated.getModifiedDate()).isEqualTo(modified);
    }

    @Test
    void updateWithoutNewImagesKeepsExistingImages() throws Exception {
        seedImage("https://cdn.example.com/a.jpg");
        seedImage("https://cdn.example.com/b.jpg");

        Post updated = inRequest(() -> postService(postRepository)
                .updatePost(member, postId, update("new title", null), Collections.emptyList()));

        assertThat(activeImages()).isEqualTo(2);
        assertThat(updated.getPostImageList()).hasSize(2);
        assertThat(updated.getTitle()).isEqualTo("new title");
        assertThat(updated.getContent()).isEqualTo("content");
    }

    @Test
    void updateWithNewImageAddsItAndReturnsFreshPost() throws Exception {
        seedImage("https://cdn.example.com/a.jpg");
        when(s3ImageService.uploadPostImage(any(), anyString())).thenReturn("https://cdn.example.com/new.jpg");
        MockMultipartFile image = new MockMultipartFile("images", "new.jpg", "image/jpeg", new byte[]{1, 2, 3});

        Post updated = inRequest(() -> postService(postRepository)
                .updatePost(member, postId, update("new title", "new content"), Collections.singletonList(image)));

        assertThat(activeImages()).isEqualTo(2);
        // 같은 요청(EntityManager) 안에서 다시 조회해도 바뀐 제목과 새 이미지가 보여야 한다.
        assertThat(updated.getTitle()).isEqualTo("new title");
        assertThat(updated.getPostImageList()).hasSize(2)
                .extracting(PostImage::getAddress).contains("https://cdn.example.com/new.jpg");
    }

    @Test
    void updateDoesNotTouchPostDeletedMeanwhile() throws Exception {
        AtomicBoolean interleaved = new AtomicBoolean();
        PostRepository racingPosts = afterFirstCall(PostRepository.class, postRepository, "findById", interleaved, () ->
                // 수정 요청이 게시글을 읽은 직후 게시글이 삭제된 상황(작성자 삭제·차단·신고 자동 삭제)
                jdbc.update("UPDATE posts SET deleted = 1 WHERE id = ?", postId));

        assertThatThrownBy(() -> inRequest(() -> postService(racingPosts)
                .updatePost(member, postId, update("new title", null), Collections.emptyList())))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(interleaved).isTrue();
        assertThat(postDeleted()).isTrue();
        assertThat(jdbc.queryForObject("SELECT title FROM posts WHERE id = ?", String.class, postId)).isEqualTo("title");
    }

    // ---- helpers ----

    private static PostUpdate update(String title, String content) {
        return PostUpdate.builder().title(title).content(content).build();
    }

    private long reportRows(long reporterId, ReportReason reason) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post_reports WHERE reporter_id = ? AND post_id = ? "
                + "AND report_reason = ?", Long.class, reporterId, postId, reason.name());
    }

    private List<String> uniqueIndexColumns(String table, String index) {
        return jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? AND NON_UNIQUE = 0 "
                        + "ORDER BY SEQ_IN_INDEX",
                String.class, table, index);
    }

    private List<Long> ids(String table) {
        return jdbc.queryForList("SELECT id FROM " + table + " ORDER BY id", Long.class);
    }

    private static String script(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    /** 테이블 이름만 mig_*로 바꾼다. backup_126_post_reports, uk_post_reports_…는 앞에 '_'가 있어 그대로 남는다. */
    private static String onMigrationTables(String script) {
        return script.replaceAll("\\bpost_reports\\b", "mig_post_reports");
    }

    private void runScript(String script) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try {
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            } finally {
                // 스크립트가 중간에 실패해도 풀로 돌아가는 커넥션에 열린 트랜잭션·임시 테이블·바뀐 격리 수준이 남지 않게 한다.
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ROLLBACK");
                    statement.execute("DROP TEMPORARY TABLE IF EXISTS recount_126_targets");
                    statement.execute("SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ");
                }
            }
            return null;
        });
    }

    private void dropMigrationTables() {
        jdbc.execute("DROP TABLE IF EXISTS backup_126_post_reports");
        jdbc.execute("DROP TABLE IF EXISTS mig_post_reports");
    }
}
