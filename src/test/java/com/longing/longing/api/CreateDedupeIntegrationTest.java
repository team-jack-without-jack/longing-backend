package com.longing.longing.api;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.domain.Comment;
import com.longing.longing.api.comment.domain.CommentCreate;
import com.longing.longing.api.comment.infrastructure.CommentEntity;
import com.longing.longing.api.comment.infrastructure.CommentRepositoryImpl;
import com.longing.longing.api.comment.service.CommentServiceImpl;
import com.longing.longing.api.comment.service.GuardedCommentCreator;
import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.domain.PostCreate;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.post.infrastructure.PostRepositoryImpl;
import com.longing.longing.api.post.service.GuardedPostCreator;
import com.longing.longing.api.post.service.PostServiceImpl;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.api.user.infrastructure.UserJpaRepository;
import com.longing.longing.api.user.service.port.UserRepository;
import com.longing.longing.common.ErrorCode;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.common.service.port.PostImageRepository;
import com.longing.longing.common.writeguard.CreateGuardExecutor;
import com.longing.longing.common.writeguard.CreateOutcome;
import com.longing.longing.common.writeguard.IdempotencyKey;
import com.longing.longing.common.writeguard.RedisWriteGuard;
import com.longing.longing.common.writeguard.WriteGuard;
import com.longing.longing.common.writeguard.WriteGuardException;
import com.longing.longing.common.writeguard.WriteGuardProperties;
import com.longing.longing.config.QueryDslConfig;
import com.longing.longing.support.RedisTestSupport;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 동시성 테스트는 THREADS(8)개 스레드가 각자 커넥션을 하나씩 쥔다(OSIV 변형은 호출 전체 동안).
// 스레드 수를 늘리면 maximum-pool-size도 함께 늘린다.
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MariaDB106Dialect",
        "spring.datasource.hikari.maximum-pool-size=10"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({QueryDslConfig.class, PostRepositoryImpl.class, CommentRepositoryImpl.class,
        PostServiceImpl.class, CommentServiceImpl.class})
class CreateDedupeIntegrationTest {

    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.6"));
    static final GenericContainer<?> REDIS;

    private static final int THREADS = 8;
    /** CreateGuardExecutor.METRIC(패키지 전용)과 같은 이름. */
    private static final String DECISIONS = "write.guard.decisions";

    static {
        MARIADB.start();
        REDIS = RedisTestSupport.startRedis();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
    }

    @MockBean S3ImageService s3ImageService;
    @MockBean PostImageRepository postImageRepository;
    @MockBean UserRepository userRepository;

    @Autowired PostService postService;
    @Autowired CommentService commentService;
    @Autowired UserJpaRepository userJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;

    private User member;
    private RedisWriteGuard redisGuard;
    private SimpleMeterRegistry meters;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                try {
                    for (String table : Arrays.asList("comments", "posts", "users")) {
                        statement.execute("TRUNCATE TABLE " + table);
                    }
                } finally {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }
            return null;
        });
        RedisTestSupport.flush(REDIS);
        meters = new SimpleMeterRegistry();
        redisGuard = RedisTestSupport.patientGuard(REDIS);
        member = saveUser("member@example.com", "member-1");
    }

    @AfterEach
    void tearDown() {
        if (redisGuard != null) {
            redisGuard.close();
        }
    }

    // ---- 키 없음 ----

    @Test
    void keylessRapidDoubleSubmitCreatesOnePostAndOneComment() {
        GuardedPostCreator posts = postCreator(redisGuard);
        PostCreate create = postCreate("title", "content");
        Post post = posts.create(member, create, null, Optional.empty()).getValue();
        assertRejected(() -> posts.create(member, create, null, Optional.empty()), ErrorCode.DUPLICATE_SUBMISSION);

        GuardedCommentCreator comments = commentCreator(redisGuard);
        CommentCreate comment = commentCreate(post.getId(), "hello");
        comments.create(member, comment, Optional.empty());
        assertRejected(() -> comments.create(member, comment, Optional.empty()), ErrorCode.DUPLICATE_SUBMISSION);

        assertThat(count("posts")).isEqualTo(1);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(commentCount(post.getId())).isEqualTo(1);
    }

    @Test
    void concurrentKeylessCommentsCreateOne() throws Exception {
        Post post = seedPost();
        GuardedCommentCreator comments = commentCreator(redisGuard);

        List<Object> results = runConcurrently(THREADS,
                () -> comments.create(member, commentCreate(post.getId(), "hello"), Optional.empty()));

        assertThat(unexpected(results)).isEmpty();
        assertThat(outcomes(results)).hasSize(1);
        assertThat(errorCodes(results)).hasSize(THREADS - 1).containsOnly(ErrorCode.DUPLICATE_SUBMISSION);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(commentCount(post.getId())).isEqualTo(1);
        assertThat(decisions("available")).isEqualTo(THREADS);
        assertThat(decisions("unavailable")).isZero();
    }

    @Test
    void concurrentKeylessPostsCreateOne() throws Exception {
        GuardedPostCreator posts = postCreator(redisGuard);

        List<Object> results = runConcurrently(THREADS,
                () -> posts.create(member, postCreate("title", "content"), null, Optional.empty()));

        assertThat(unexpected(results)).isEmpty();
        assertThat(outcomes(results)).hasSize(1);
        assertThat(errorCodes(results)).hasSize(THREADS - 1).containsOnly(ErrorCode.DUPLICATE_SUBMISSION);
        assertThat(count("posts")).isEqualTo(1);
        assertThat(decisions("available")).isEqualTo(THREADS);
        assertThat(decisions("unavailable")).isZero();
    }

    @Test
    void keylessRequestsDoNotCollideOnNullKeys() {
        GuardedPostCreator posts = postCreator(redisGuard);

        posts.create(member, postCreate("first", "content"), null, Optional.empty());
        posts.create(member, postCreate("second", "content"), null, Optional.empty());

        assertThat(count("posts")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE client_request_id IS NULL", Long.class))
                .isEqualTo(2L);
    }

    // ---- 키 있음 ----
    // 동시 같은 키 테스트는 모든 스레드가 첫 조회를 마친 뒤에야 생성으로 넘어간다(racing*Creator).
    // 그래서 Redis가 있으면 잠금을 못 얻은 7개는 반드시 REQUEST_IN_PROGRESS,
    // Redis가 없으면 7개 모두 unique 위반 → 재조회 → 재생 경로를 지난다.

    @Test
    void concurrentSameKeyCommentsWithRedisCreateOne() throws Exception {
        Post post = seedPost();
        GuardedCommentCreator comments = racingCommentCreator(redisGuard);

        List<Object> results = runConcurrently(THREADS,
                () -> comments.create(member, commentCreate(post.getId(), "hello"), key("key-1")));

        assertThat(unexpected(results)).isEmpty();
        assertThat(created(results)).isEqualTo(1);
        assertThat(replayed(results)).isZero();
        assertThat(errorCodes(results)).hasSize(THREADS - 1).containsOnly(ErrorCode.REQUEST_IN_PROGRESS);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(commentCount(post.getId())).isEqualTo(1);
        assertThat(decisions("available")).isEqualTo(THREADS);
        assertThat(decisions("unavailable")).isZero();
    }

    @Test
    void concurrentSameKeyCommentsWithRedisDownCreateOneAndReplayTheRest() throws Exception {
        Post post = seedPost();
        GuardedCommentCreator comments = racingCommentCreator(RedisTestSupport.UNAVAILABLE);

        List<Object> results = runConcurrently(THREADS,
                () -> comments.create(member, commentCreate(post.getId(), "hello"), key("key-1")));

        assertThat(unexpected(results)).isEmpty();
        assertThat(errorCodes(results)).isEmpty();
        assertThat(created(results)).isEqualTo(1);
        assertThat(replayed(results)).isEqualTo(THREADS - 1);
        assertThat(distinctIds(results)).isEqualTo(1);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(commentCount(post.getId())).isEqualTo(1);
        assertThat(decisions("unavailable")).isEqualTo(THREADS);
    }

    @Test
    void concurrentSameKeyPostsWithRedisDownCreateOneAndReplayTheRest() throws Exception {
        GuardedPostCreator posts = racingPostCreator(RedisTestSupport.UNAVAILABLE);

        List<Object> results = runConcurrently(THREADS,
                () -> posts.create(member, postCreate("title", "content"), null, key("key-1")));

        assertThat(unexpected(results)).isEmpty();
        assertThat(errorCodes(results)).isEmpty();
        assertThat(created(results)).isEqualTo(1);
        assertThat(replayed(results)).isEqualTo(THREADS - 1);
        assertThat(distinctIds(results)).isEqualTo(1);
        assertThat(count("posts")).isEqualTo(1);
        assertThat(decisions("unavailable")).isEqualTo(THREADS);
    }

    @Test
    void concurrentSameKeyPostsUnderOpenEntityManagerInViewCreateOne() throws Exception {
        GuardedPostCreator posts = racingPostCreator(RedisTestSupport.UNAVAILABLE);

        List<Object> results = runConcurrently(THREADS, () -> withOpenEntityManager(
                () -> posts.create(member, postCreate("title", "content"), null, key("key-1"))));

        assertThat(unexpected(results)).isEmpty();
        assertThat(errorCodes(results)).isEmpty();
        assertThat(created(results)).isEqualTo(1);
        assertThat(replayed(results)).isEqualTo(THREADS - 1);
        assertThat(distinctIds(results)).isEqualTo(1);
        assertThat(count("posts")).isEqualTo(1);
        assertThat(decisions("unavailable")).isEqualTo(THREADS);
    }

    @Test
    void concurrentSameKeyCommentsUnderOpenEntityManagerInViewCreateOne() throws Exception {
        Post post = seedPost();
        GuardedCommentCreator comments = racingCommentCreator(RedisTestSupport.UNAVAILABLE);

        List<Object> results = runConcurrently(THREADS, () -> withOpenEntityManager(
                () -> comments.create(member, commentCreate(post.getId(), "hello"), key("key-1"))));

        assertThat(unexpected(results)).isEmpty();
        assertThat(errorCodes(results)).isEmpty();
        assertThat(created(results)).isEqualTo(1);
        assertThat(replayed(results)).isEqualTo(THREADS - 1);
        assertThat(distinctIds(results)).isEqualTo(1);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(commentCount(post.getId())).isEqualTo(1);
        assertThat(decisions("unavailable")).isEqualTo(THREADS);
    }

    @Test
    void sameKeyWithDifferentContentIsRejectedWithoutCreating() {
        GuardedPostCreator posts = postCreator(redisGuard);
        posts.create(member, postCreate("title", "content"), null, key("key-1"));

        assertRejected(() -> posts.create(member, postCreate("title", "changed"), null, key("key-1")),
                ErrorCode.IDEMPOTENCY_KEY_REUSED);
        assertThat(count("posts")).isEqualTo(1);
    }

    @Test
    void sameKeyCommentWithDifferentContentIsRejectedWithoutCreating() {
        Post post = seedPost();
        GuardedCommentCreator comments = commentCreator(redisGuard);
        comments.create(member, commentCreate(post.getId(), "hello"), key("key-1"));

        assertRejected(() -> comments.create(member, commentCreate(post.getId(), "changed"), key("key-1")),
                ErrorCode.IDEMPOTENCY_KEY_REUSED);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(commentCount(post.getId())).isEqualTo(1);
    }

    @Test
    void keyOfSoftDeletedPostIsRejectedAsReused() {
        GuardedPostCreator posts = postCreator(redisGuard);
        Post created = posts.create(member, postCreate("title", "content"), null, key("key-1")).getValue();
        // PostEntity @SQLDelete와 같은 soft delete. (PostServiceImpl.deletePost는 트랜잭션이 없어 OSIV 없는 테스트에서 지연 로딩이 실패한다.)
        jdbc.update("UPDATE posts SET deleted = true, deleted_date = NOW() WHERE id = ?", created.getId());

        assertRejected(() -> posts.create(member, postCreate("title", "content"), null, key("key-1")),
                ErrorCode.IDEMPOTENCY_KEY_REUSED);
        assertThat(count("posts")).isEqualTo(1);
    }

    @Test
    void keyOfSoftDeletedCommentIsRejectedAsReused() {
        Post post = seedPost();
        GuardedCommentCreator comments = commentCreator(redisGuard);
        Comment created = comments.create(member, commentCreate(post.getId(), "hello"), key("key-1")).getValue();
        // CommentEntity @SQLDelete와 같은 soft delete.
        jdbc.update("UPDATE comments SET deleted = true, deleted_date = NOW() WHERE id = ?", created.getId());

        assertRejected(() -> comments.create(member, commentCreate(post.getId(), "hello"), key("key-1")),
                ErrorCode.IDEMPOTENCY_KEY_REUSED);
        assertThat(count("comments")).isEqualTo(1);
        // 실패한 생성의 comment_count 증가도 함께 롤백된다.
        assertThat(commentCount(post.getId())).isEqualTo(1);
    }

    @Test
    void keysDifferingOnlyByCaseAreDistinctLikeInRedis() {
        GuardedPostCreator posts = postCreator(redisGuard);

        posts.create(member, postCreate("title", "content"), null, key("Key-1"));
        posts.create(member, postCreate("title", "content"), null, key("key-1"));

        assertThat(count("posts")).isEqualTo(2);
    }

    @Test
    void sameKeyFromDifferentUsersCreatesSeparatePosts() {
        User other = saveUser("other@example.com", "other-1");
        GuardedPostCreator posts = postCreator(redisGuard);

        posts.create(member, postCreate("title", "content"), null, key("key-1"));
        posts.create(other, postCreate("title", "content"), null, key("key-1"));

        assertThat(count("posts")).isEqualTo(2);
    }

    // ---- 스키마·마이그레이션 ----

    @Test
    void hibernateSchemaHasTheMigrationUniqueIndexes() {
        assertThat(uniqueIndexColumns("posts", PostEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT))
                .containsExactly("user_id", "client_request_id");
        assertThat(uniqueIndexColumns("comments", CommentEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT))
                .containsExactly("user_id", "client_request_id");
        assertThat(keyCollation("posts")).isEqualTo("ascii_bin");
        assertThat(keyCollation("comments")).isEqualTo("ascii_bin");
    }

    @Test
    void migrationAndRollbackScriptsMatchTheEntitySchema() throws Exception {
        String forward = script("scripts/sql/122-create-dedupe.sql");
        String rollback = script("scripts/sql/122-create-dedupe-rollback.sql");
        assertThat(forward).contains("ALTER TABLE posts").contains("ALTER TABLE comments");
        assertThat(rollback).contains("ALTER TABLE posts").contains("ALTER TABLE comments");
        // 같은 스크립트를 이전 스키마 모양(user_id 외래 키 포함)의 별도 테이블에 적용한다(인덱스 이름은 테이블별이라 충돌하지 않는다).
        dropMigrationTables();
        try {
            jdbc.execute("CREATE TABLE mig_posts (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, "
                    + "CONSTRAINT fk_mig_posts_user FOREIGN KEY (user_id) REFERENCES users (id))");
            jdbc.execute("CREATE TABLE mig_comments (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, "
                    + "CONSTRAINT fk_mig_comments_user FOREIGN KEY (user_id) REFERENCES users (id))");
            jdbc.update("INSERT INTO mig_posts (user_id) VALUES (?), (?)", member.getId(), member.getId());
            jdbc.update("INSERT INTO mig_comments (user_id) VALUES (?), (?)", member.getId(), member.getId());

            runScript(onMigrationTables(forward));

            // 드리프트 방지: 운영 스크립트가 만든 컬럼·인덱스가 엔티티(Hibernate)가 만든 것과 같아야 한다.
            assertThat(keyColumn("mig_posts")).isEqualTo(keyColumn("posts"));
            assertThat(keyColumn("mig_comments")).isEqualTo(keyColumn("comments"));
            assertThat(uniqueIndexColumns("mig_posts", PostEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT))
                    .isEqualTo(uniqueIndexColumns("posts", PostEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT))
                    .containsExactly("user_id", "client_request_id");
            assertThat(uniqueIndexColumns("mig_comments", CommentEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT))
                    .isEqualTo(uniqueIndexColumns("comments", CommentEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT))
                    .containsExactly("user_id", "client_request_id");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mig_posts WHERE client_request_id IS NULL", Long.class))
                    .isEqualTo(2L);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mig_comments WHERE client_request_id IS NULL", Long.class))
                    .isEqualTo(2L);
            jdbc.update("INSERT INTO mig_posts (user_id, client_request_id) VALUES (?, 'key-1')", member.getId());
            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO mig_posts (user_id, client_request_id) VALUES (?, 'key-1')", member.getId()))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining(PostEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT);

            // 재실행해도 실패하지 않는다.
            runScript(onMigrationTables(forward));

            runScript(onMigrationTables(rollback));

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('mig_posts', 'mig_comments') "
                    + "AND COLUMN_NAME = 'client_request_id'", Long.class)).isZero();
            assertThat(uniqueIndexColumns("mig_posts", PostEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT)).isEmpty();
            assertThat(uniqueIndexColumns("mig_comments", CommentEntity.CLIENT_REQUEST_UNIQUE_CONSTRAINT)).isEmpty();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS "
                    + "WHERE CONSTRAINT_SCHEMA = DATABASE() "
                    + "AND CONSTRAINT_NAME IN ('fk_mig_posts_user', 'fk_mig_comments_user')", Long.class))
                    .isEqualTo(2L);
        } finally {
            dropMigrationTables();
        }
    }

    // ---- helpers ----

    private GuardedPostCreator postCreator(WriteGuard guard) {
        return new GuardedPostCreator(postService, executor(guard));
    }

    private GuardedCommentCreator commentCreator(WriteGuard guard) {
        return new GuardedCommentCreator(commentService, executor(guard));
    }

    /** THREADS개 스레드가 모두 첫 키 조회를 마칠 때까지 생성으로 넘어가지 않는다. */
    private GuardedPostCreator racingPostCreator(WriteGuard guard) {
        return new GuardedPostCreator(afterFirstLookupBarrier(PostService.class, postService), executor(guard));
    }

    private GuardedCommentCreator racingCommentCreator(WriteGuard guard) {
        return new GuardedCommentCreator(afterFirstLookupBarrier(CommentService.class, commentService),
                executor(guard));
    }

    private CreateGuardExecutor executor(WriteGuard guard) {
        return new CreateGuardExecutor(guard, new WriteGuardProperties(), meters);
    }

    /**
     * 각 스레드의 첫 findByClientRequestId가 실제 조회를 마친 뒤 THREADS개 스레드가 모두 도착할 때까지 기다린다.
     * 이후 호출(충돌 뒤 재조회)은 기다리지 않는다.
     */
    private static <S> S afterFirstLookupBarrier(Class<S> serviceType, S target) {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        ThreadLocal<Boolean> waited = ThreadLocal.withInitial(() -> Boolean.FALSE);
        InvocationHandler handler = (proxy, method, args) -> {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            if (method.getName().equals("findByClientRequestId") && !waited.get()) {
                waited.set(Boolean.TRUE);
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                    throw new IllegalStateException("not all " + THREADS + " threads finished their first lookup", e);
                }
            }
            return result;
        };
        return serviceType.cast(Proxy.newProxyInstance(
                serviceType.getClassLoader(), new Class<?>[]{serviceType}, handler));
    }

    private User saveUser(String email, String providerId) {
        return userJpaRepository.save(UserEntity.builder()
                .name(email).email(email)
                .provider(Provider.GOOGLE).providerId(providerId).role(Role.GUEST)
                .build()).toModel();
    }

    private Post seedPost() {
        return postService.createPost(member, postCreate("seed", "seed"), null, null);
    }

    private static PostCreate postCreate(String title, String content) {
        return PostCreate.builder().title(title).content(content).build();
    }

    private static CommentCreate commentCreate(long postId, String content) {
        return CommentCreate.builder().postId(postId).content(content).build();
    }

    private static Optional<IdempotencyKey> key(String value) {
        return IdempotencyKey.parse(value);
    }

    private static void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(expected);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private int commentCount(long postId) {
        return jdbc.queryForObject("SELECT comment_count FROM posts WHERE id = ?", Integer.class, postId);
    }

    private long decisions(String redis) {
        return (long) meters.find(DECISIONS).tag("redis", redis).counters().stream()
                .mapToDouble(Counter::count).sum();
    }

    private List<String> uniqueIndexColumns(String table, String index) {
        return jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? AND NON_UNIQUE = 0 "
                        + "ORDER BY SEQ_IN_INDEX",
                String.class, table, index);
    }

    private String keyCollation(String table) {
        return jdbc.queryForObject("SELECT COLLATION_NAME FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'client_request_id'",
                String.class, table);
    }

    private Map<String, Object> keyColumn(String table) {
        return jdbc.queryForMap("SELECT COLUMN_TYPE, CHARACTER_SET_NAME, COLLATION_NAME, IS_NULLABLE "
                        + "FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'client_request_id'",
                table);
    }

    private static String script(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    /** 테이블 이름만 mig_*로 바꾼다(uk_posts_... 같은 인덱스 이름은 단어 경계가 아니라 그대로 남는다). */
    private static String onMigrationTables(String script) {
        return script.replaceAll("\\bposts\\b", "mig_posts").replaceAll("\\bcomments\\b", "mig_comments");
    }

    private void runScript(String script) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(
                    new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            return null;
        });
    }

    private void dropMigrationTables() {
        jdbc.execute("DROP TABLE IF EXISTS mig_comments");
        jdbc.execute("DROP TABLE IF EXISTS mig_posts");
    }

    /** 운영의 open-in-view(OpenEntityManagerInViewInterceptor)처럼 요청 스레드에 EntityManager를 미리 묶는다. */
    private <T> T withOpenEntityManager(Callable<T> task) throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try {
            T result = task.call();
            // 서비스 트랜잭션이 새 EntityManager가 아니라 이 스레드에 묶인 것을 실제로 썼다.
            assertThat(entityManager.unwrap(Session.class).getStatistics().getEntityCount()).isPositive();
            return result;
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            entityManager.close();
        }
    }

    private static List<Object> runConcurrently(int threads, Callable<?> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<CreateOutcome<?>> outcomes(List<Object> results) {
        return results.stream().filter(r -> r instanceof CreateOutcome)
                .map(r -> (CreateOutcome<?>) r).collect(Collectors.toList());
    }

    private static List<ErrorCode> errorCodes(List<Object> results) {
        return results.stream().filter(r -> r instanceof WriteGuardException)
                .map(r -> ((WriteGuardException) r).getErrorCode()).collect(Collectors.toList());
    }

    private static List<Object> unexpected(List<Object> results) {
        return results.stream()
                .filter(r -> !(r instanceof CreateOutcome) && !(r instanceof WriteGuardException))
                .collect(Collectors.toList());
    }

    private static long created(List<Object> results) {
        return outcomes(results).stream().filter(o -> !o.isReplayed()).count();
    }

    private static long replayed(List<Object> results) {
        return outcomes(results).stream().filter(o -> o.isReplayed()).count();
    }

    private static long distinctIds(List<Object> results) {
        return outcomes(results).stream().map(o -> idOf(o.getValue())).distinct().count();
    }

    private static Long idOf(Object value) {
        return value instanceof Post ? ((Post) value).getId() : ((Comment) value).getId();
    }
}
