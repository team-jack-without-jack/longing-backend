package com.longing.longing.api;

import com.longing.longing.api.post.domain.Post;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.post.infrastructure.PostJpaRepository;
import com.longing.longing.api.post.infrastructure.PostRepositoryImpl;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.controller.port.UserService;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.infrastructure.UserBlockEntity;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.api.user.infrastructure.UserJpaRepository;
import com.longing.longing.api.user.infrastructure.UserRepositoryImpl;
import com.longing.longing.api.user.service.UserServiceImpl;
import com.longing.longing.api.user.service.port.UserRepository;
import com.longing.longing.bookmark.controller.port.BookmarkService;
import com.longing.longing.bookmark.domain.PostBookmark;
import com.longing.longing.bookmark.infrastructure.BookmarkRepositoryImpl;
import com.longing.longing.bookmark.infrastructure.PostBookmarkEntity;
import com.longing.longing.bookmark.service.BookmarkServiceImpl;
import com.longing.longing.bookmark.service.port.BookmarkRepository;
import com.longing.longing.common.service.S3ImageServiceImpl;
import com.longing.longing.config.QueryDslConfig;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

// 동시성 테스트는 THREADS(8)개 스레드가 각자 커넥션을 하나씩 쥔다(open-in-view 변형은 호출 전체 동안).
// 스레드 수를 늘리면 maximum-pool-size도 함께 늘린다.
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MariaDB106Dialect",
        "spring.datasource.hikari.maximum-pool-size=10"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({QueryDslConfig.class, PostRepositoryImpl.class, BookmarkRepositoryImpl.class, UserRepositoryImpl.class,
        BookmarkServiceImpl.class, UserServiceImpl.class})
class BookmarkBlockDedupeIntegrationTest {

    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.6"));

    private static final int THREADS = 8;

    static {
        MARIADB.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
    }

    @MockBean S3ImageServiceImpl s3ImageService;

    @Autowired BookmarkService bookmarkService;
    @Autowired UserService userService;
    @Autowired BookmarkRepository bookmarkRepository;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired UserJpaRepository userJpaRepository;
    @Autowired PostJpaRepository postJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;

    private User member;
    private User other;
    private long postId;

    @BeforeEach
    void setUp() {
        truncate("post_bookmarks", "user_blocks", "post_likes", "comments", "posts", "users");
        // 게시글 id가 사용자 id와 겹치지 않게 해서 (userId, postId) 인자 순서 실수를 테스트가 잡게 한다.
        jdbc.execute("ALTER TABLE posts AUTO_INCREMENT = 1001");
        member = saveUser("member@example.com", "member-1");
        other = saveUser("other@example.com", "other-1");
        postId = seedPost(member);
    }

    // ---- 북마크 ----
    // 북마크 경로는 PostEntity.toModel()이 user를 지연 로딩하므로, 운영(open-in-view)처럼 inRequest 안에서 호출한다.

    @Test
    void concurrentBookmarksLeaveOneRow() throws Exception {
        List<Object> inserts = Collections.synchronizedList(new ArrayList<>());

        List<Object> results = addBookmarksConcurrently(
                racing(BookmarkRepository.class, bookmarkRepository, "exists", "saveIfAbsent", inserts));

        assertThat(failures(results)).isEmpty();
        // 모든 요청이 삽입까지 갔고, 1개만 삽입되고 7개는 unique 위반으로 "이미 있음"이 됐다.
        assertThat(inserts).hasSize(THREADS);
        assertThat(inserts).filteredOn(Boolean.FALSE::equals).hasSize(THREADS - 1);
        // 진 요청도 롤백 뒤 같은 요청 안에서 이어서 조회했고, 북마크된 게시글을 받았다.
        assertThat(results).allSatisfy(post -> assertThat(((Post) post).isBookmarked()).isTrue());
        assertThat(bookmarkRows(member.getId(), postId)).isEqualTo(1);
    }

    @Test
    void removeBookmarkAfterConcurrentAddsSucceeds() throws Exception {
        List<Object> adds = addBookmarksConcurrently(racing(BookmarkRepository.class, bookmarkRepository,
                "exists", "saveIfAbsent", Collections.synchronizedList(new ArrayList<>())));
        assertThat(failures(adds)).isEmpty();
        assertThat(bookmarkRows(member.getId(), postId)).isPositive();

        unbookmark(member, postId);

        assertThat(bookmarkRows(member.getId(), postId)).isZero();
    }

    @Test
    void concurrentBookmarkRemovalsAllSucceed() throws Exception {
        bookmark(member, postId);
        assertThat(bookmarkRows(member.getId(), postId)).isEqualTo(1);

        List<Object> results = runConcurrently(THREADS, () -> {
            unbookmark(member, postId);
            return "ok";
        });

        assertThat(failures(results)).isEmpty();
        assertThat(bookmarkRows(member.getId(), postId)).isZero();
    }

    @Test
    void bookmarkCanBeAddedAgainAfterRemoval() throws Exception {
        bookmark(member, postId);
        assertThat(bookmarkRows(member.getId(), postId)).isEqualTo(1);
        unbookmark(member, postId);
        assertThat(bookmarkRows(member.getId(), postId)).isZero();
        bookmark(member, postId);

        assertThat(bookmarkRows(member.getId(), postId)).isEqualTo(1);
    }

    @Test
    void bookmarksOfDifferentUsersAndPostsAreIndependent() throws Exception {
        long otherPostId = seedPost(other);

        bookmark(member, postId);
        bookmark(member, otherPostId);
        bookmark(other, postId);
        assertThat(bookmarkRows(other.getId(), postId)).isEqualTo(1);
        unbookmark(other, postId);

        assertThat(bookmarkRows(member.getId(), postId)).isEqualTo(1);
        assertThat(bookmarkRows(member.getId(), otherPostId)).isEqualTo(1);
        assertThat(bookmarkRows(other.getId(), postId)).isZero();
    }

    @Test
    void bookmarkForeignKeyViolationIsNotHiddenAsDuplicate() {
        Post missing = Post.builder().id(Long.MAX_VALUE).title("missing").content("missing").user(member).build();

        assertThatThrownBy(() -> bookmarkRepository.saveIfAbsent(
                PostBookmark.builder().post(missing).user(member).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(count("post_bookmarks")).isZero();
    }

    // ---- 차단 ----

    @Test
    void repeatedBlockLeavesOneRow() throws Exception {
        block(member, other.getId());
        block(member, other.getId());

        assertThat(blockRows(member.getId(), other.getId())).isEqualTo(1);
    }

    @Test
    void unblockAfterRepeatedBlockSucceeds() throws Exception {
        block(member, other.getId());
        block(member, other.getId());
        assertThat(blockRows(member.getId(), other.getId())).isPositive();

        unblock(member, other.getId());

        assertThat(blockRows(member.getId(), other.getId())).isZero();
    }

    @Test
    void concurrentBlocksLeaveOneRowAndAllSucceed() throws Exception {
        List<Object> inserts = Collections.synchronizedList(new ArrayList<>());
        UserService racing = UserServiceImpl.builder()
                .userRepository(racing(UserRepository.class, userRepository, "isBlocked", "blockIfAbsent", inserts))
                .s3ImageService(s3ImageService)
                .build();

        // 운영(open-in-view)처럼 요청 범위 안에서 차단하고, 같은 요청에서 이어서 조회한다.
        // 진 요청의 삽입이 롤백된 뒤에도 같은 EntityManager를 계속 쓸 수 있어야 한다.
        List<Object> results = runConcurrently(THREADS, () -> inRequest(() -> {
            racing.blockUser(member, other.getId());
            userRepository.findById(other.getId()).orElseThrow(IllegalStateException::new);
            return userRepository.isBlocked(member.getId(), other.getId());
        }));

        assertThat(failures(results)).isEmpty();
        assertThat(results).containsOnly(true);
        assertThat(inserts).hasSize(THREADS);
        assertThat(inserts).filteredOn(Boolean.FALSE::equals).hasSize(THREADS - 1);
        assertThat(blockRows(member.getId(), other.getId())).isEqualTo(1);
    }

    @Test
    void concurrentUnblocksAllSucceed() throws Exception {
        block(member, other.getId());
        assertThat(blockRows(member.getId(), other.getId())).isEqualTo(1);

        List<Object> results = runConcurrently(THREADS, () -> {
            unblock(member, other.getId());
            return "ok";
        });

        assertThat(failures(results)).isEmpty();
        assertThat(blockRows(member.getId(), other.getId())).isZero();
    }

    @Test
    void userCanBeBlockedAgainAfterUnblock() throws Exception {
        block(member, other.getId());
        assertThat(blockRows(member.getId(), other.getId())).isEqualTo(1);
        unblock(member, other.getId());
        assertThat(blockRows(member.getId(), other.getId())).isZero();
        block(member, other.getId());

        assertThat(blockRows(member.getId(), other.getId())).isEqualTo(1);
    }

    @Test
    void blocksAreDirectional() throws Exception {
        block(member, other.getId());
        block(other, member.getId());
        assertThat(blockRows(other.getId(), member.getId())).isEqualTo(1);
        unblock(other, member.getId());

        assertThat(blockRows(member.getId(), other.getId())).isEqualTo(1);
        assertThat(blockRows(other.getId(), member.getId())).isZero();
    }

    @Test
    void blockForeignKeyViolationIsNotHiddenAsDuplicate() {
        User missing = User.builder().id(Long.MAX_VALUE).name("missing").email("missing@example.com")
                .provider(Provider.GOOGLE).providerId("missing").role(Role.GUEST).build();

        assertThatThrownBy(() -> userRepository.blockIfAbsent(member, missing))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(count("user_blocks")).isZero();
    }

    // ---- 마이그레이션 전 중복 데이터 ----

    @Test
    void bookmarkRemovalToleratesDuplicateRows() throws Exception {
        withoutUniqueIndex("post_bookmarks", PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT, "user_id, post_id", () -> {
            insertBookmarkRow(member.getId(), postId);
            insertBookmarkRow(member.getId(), postId);

            // 중복이 있어도 게시글 조회는 된다(Hibernate가 같은 Boolean.TRUE 결과를 하나로 본다).
            assertThat(inRequest(() -> postRepository.findById(postId, member.getId())))
                    .hasValueSatisfying(post -> assertThat(post.isBookmarked()).isTrue());

            unbookmark(member, postId);
            assertThat(bookmarkRows(member.getId(), postId)).isZero();
        });
    }

    @Test
    void unblockRemovesDuplicateBlockRows() throws Exception {
        withoutUniqueIndex("user_blocks", UserBlockEntity.USER_BLOCKED_UNIQUE_CONSTRAINT, "user_id, blocked_user_id", () -> {
            insertBlockRow(member.getId(), other.getId());
            insertBlockRow(member.getId(), other.getId());

            assertThat(userRepository.isBlocked(member.getId(), other.getId())).isTrue();

            unblock(member, other.getId());
            assertThat(blockRows(member.getId(), other.getId())).isZero();
        });
    }

    // ---- 스키마 ----

    @Test
    void hibernateSchemaHasTheUniqueIndexes() {
        assertThat(uniqueIndexColumns("post_bookmarks", PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT))
                .containsExactly("user_id", "post_id");
        assertThat(uniqueIndexColumns("user_blocks", UserBlockEntity.USER_BLOCKED_UNIQUE_CONSTRAINT))
                .containsExactly("user_id", "blocked_user_id");
    }

    @Test
    void migrationCleansDuplicatesAddsConstraintsAndRollsBack() throws Exception {
        String forward = script("scripts/sql/124-bookmark-block-unique.sql");
        String rollback = script("scripts/sql/124-bookmark-block-unique-rollback.sql");
        assertThat(forward).contains("ALTER TABLE post_bookmarks").contains("ALTER TABLE user_blocks");
        assertThat(rollback).contains("ALTER TABLE post_bookmarks").contains("ALTER TABLE user_blocks");
        long m = member.getId();
        long o = other.getId();
        long secondPostId = seedPost(other);

        // 같은 스크립트를 이전 스키마 모양(BaseTimeEntity 컬럼·외래 키 포함, unique 없음)의 별도 테이블에 적용한다.
        dropMigrationTables();
        try {
            jdbc.execute("CREATE TABLE mig_post_bookmarks (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "created_date DATETIME(6), modified_date DATETIME(6), "
                    + "deleted TINYINT(1) NOT NULL DEFAULT 0, deleted_date DATETIME(6), "
                    + "user_id BIGINT, post_id BIGINT, "
                    + "CONSTRAINT fk_mig_pb_user FOREIGN KEY (user_id) REFERENCES users (id), "
                    + "CONSTRAINT fk_mig_pb_post FOREIGN KEY (post_id) REFERENCES posts (id))");
            jdbc.execute("CREATE TABLE mig_user_blocks (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "created_date DATETIME(6), modified_date DATETIME(6), "
                    + "deleted TINYINT(1) NOT NULL DEFAULT 0, deleted_date DATETIME(6), "
                    + "user_id BIGINT, blocked_user_id BIGINT, "
                    + "CONSTRAINT fk_mig_ub_user FOREIGN KEY (user_id) REFERENCES users (id), "
                    + "CONSTRAINT fk_mig_ub_blocked FOREIGN KEY (blocked_user_id) REFERENCES users (id))");
            // 7·8번은 post_id가 NULL이라 unique 비교 대상이 아니다(둘 다 남아야 한다).
            jdbc.update("INSERT INTO mig_post_bookmarks (id, user_id, post_id) VALUES "
                            + "(1, ?, ?), (2, ?, ?), (3, ?, ?), (4, ?, ?), (5, ?, ?), (6, ?, ?), (7, ?, NULL), (8, ?, NULL)",
                    m, postId, m, postId, m, postId, m, secondPostId, o, postId, o, postId, m, m);
            jdbc.update("INSERT INTO mig_user_blocks (id, user_id, blocked_user_id) VALUES (1, ?, ?), (2, ?, ?), (3, ?, ?)",
                    m, o, m, o, o, m);

            runScript(onMigrationTables(forward));

            // (사용자, 대상)마다 가장 오래된 행만 남고, 지운 행은 백업 테이블에 있다.
            assertThat(ids("mig_post_bookmarks")).containsExactly(1L, 4L, 5L, 7L, 8L);
            assertThat(ids("mig_user_blocks")).containsExactly(1L, 3L);
            assertThat(ids("backup_124_post_bookmarks")).containsExactly(2L, 3L, 6L);
            assertThat(ids("backup_124_user_blocks")).containsExactly(2L);
            // 드리프트 방지: 운영 스크립트가 만든 인덱스가 엔티티(Hibernate)가 만든 것과 같아야 한다.
            assertThat(uniqueIndexColumns("mig_post_bookmarks", PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT))
                    .isEqualTo(uniqueIndexColumns("post_bookmarks", PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT))
                    .containsExactly("user_id", "post_id");
            assertThat(uniqueIndexColumns("mig_user_blocks", UserBlockEntity.USER_BLOCKED_UNIQUE_CONSTRAINT))
                    .isEqualTo(uniqueIndexColumns("user_blocks", UserBlockEntity.USER_BLOCKED_UNIQUE_CONSTRAINT))
                    .containsExactly("user_id", "blocked_user_id");
            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO mig_post_bookmarks (user_id, post_id) VALUES (?, ?)", m, postId))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining(PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT);
            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO mig_user_blocks (user_id, blocked_user_id) VALUES (?, ?)", m, o))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining(UserBlockEntity.USER_BLOCKED_UNIQUE_CONSTRAINT);

            // 다시 실행해도 실패하지 않고 백업이 늘지 않는다.
            runScript(onMigrationTables(forward));
            assertThat(ids("backup_124_post_bookmarks")).containsExactly(2L, 3L, 6L);
            assertThat(ids("backup_124_user_blocks")).containsExactly(2L);

            runScript(onMigrationTables(rollback));

            assertThat(uniqueIndexColumns("mig_post_bookmarks", PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT)).isEmpty();
            assertThat(uniqueIndexColumns("mig_user_blocks", UserBlockEntity.USER_BLOCKED_UNIQUE_CONSTRAINT)).isEmpty();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS "
                    + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME IN "
                    + "('fk_mig_pb_user', 'fk_mig_pb_post', 'fk_mig_ub_user', 'fk_mig_ub_blocked')", Long.class))
                    .isEqualTo(4L);

            // 제약이 없는 동안 새로 생긴 중복도 적용 스크립트를 다시 실행하면 백업·정리되고 제약이 다시 생긴다.
            // 여기서는 롤백 뒤에 중복을 넣는다. 정리와 제약 추가 사이에 중복이 생겨 3단계가 실패한 뒤 재실행하는 경우와 같은 상태다.
            jdbc.update("INSERT INTO mig_post_bookmarks (id, user_id, post_id) VALUES (9, ?, ?)", m, postId);
            jdbc.update("INSERT INTO mig_user_blocks (id, user_id, blocked_user_id) VALUES (4, ?, ?)", m, o);

            runScript(onMigrationTables(forward));

            assertThat(ids("mig_post_bookmarks")).containsExactly(1L, 4L, 5L, 7L, 8L);
            assertThat(ids("mig_user_blocks")).containsExactly(1L, 3L);
            assertThat(ids("backup_124_post_bookmarks")).containsExactly(2L, 3L, 6L, 9L);
            assertThat(ids("backup_124_user_blocks")).containsExactly(2L, 4L);
            assertThat(uniqueIndexColumns("mig_post_bookmarks", PostBookmarkEntity.USER_POST_UNIQUE_CONSTRAINT))
                    .containsExactly("user_id", "post_id");
            assertThat(uniqueIndexColumns("mig_user_blocks", UserBlockEntity.USER_BLOCKED_UNIQUE_CONSTRAINT))
                    .containsExactly("user_id", "blocked_user_id");
        } finally {
            dropMigrationTables();
        }
    }

    // ---- helpers ----

    private User saveUser(String email, String providerId) {
        return userJpaRepository.save(UserEntity.builder()
                .name(email).email(email)
                .provider(Provider.GOOGLE).providerId(providerId).role(Role.GUEST)
                .build()).toModel();
    }

    private long seedPost(User owner) {
        return postJpaRepository.save(PostEntity.builder()
                .title("title").content("content").user(UserEntity.fromModel(owner))
                .build()).getId();
    }

    private static CustomUserDetails details(User user) {
        return new CustomUserDetails(user.getEmail(), user.getProvider());
    }

    private void truncate(String... tables) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                try {
                    for (String table : tables) {
                        statement.execute("TRUNCATE TABLE " + table);
                    }
                } finally {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }
            return null;
        });
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private long bookmarkRows(long userId, long postId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post_bookmarks WHERE user_id = ? AND post_id = ?",
                Long.class, userId, postId);
    }

    private long blockRows(long userId, long blockedUserId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_blocks WHERE user_id = ? AND blocked_user_id = ?",
                Long.class, userId, blockedUserId);
    }

    /**
     * 각 스레드가 checkMethod를 처음 호출한 뒤, THREADS개 스레드가 모두 같은 지점에 올 때까지 기다린다.
     * 그래서 모든 요청이 "아직 없음"을 본 뒤에 삽입으로 넘어가고, unique 위반 경로를 매번 지난다.
     * insertMethod의 반환값(삽입 true / 이미 있음 false)은 insertResults에 모은다.
     */
    private static <S> S racing(Class<S> type, S target, String checkMethod, String insertMethod,
                                List<Object> insertResults) {
        // 이름 오타로 장벽이 조용히 꺼지지 않게 한다.
        for (String name : new String[]{checkMethod, insertMethod}) {
            if (Arrays.stream(type.getMethods()).noneMatch(m -> m.getName().equals(name))) {
                throw new IllegalArgumentException(type.getSimpleName() + " has no method " + name);
            }
        }
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        ThreadLocal<Boolean> waited = ThreadLocal.withInitial(() -> Boolean.FALSE);
        InvocationHandler handler = (proxy, method, args) -> {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                if (method.getName().equals(checkMethod)) {
                    barrier.reset(); // 다른 스레드가 10초를 기다리지 않고 바로 실패하게 한다.
                }
                throw e.getCause();
            }
            if (method.getName().equals(insertMethod)) {
                insertResults.add(result);
            }
            if (method.getName().equals(checkMethod) && !waited.get()) {
                waited.set(Boolean.TRUE);
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                    throw new IllegalStateException("not all " + THREADS + " threads reached " + checkMethod, e);
                }
            }
            return result;
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    /**
     * THREADS개 요청이 동시에 북마크를 추가하고, 각 요청 안에서 이어서 게시글을 조회한 결과를 돌려준다.
     * 이어서 조회하는 이유: 진 요청의 삽입이 롤백된 뒤에도 같은 요청(같은 EntityManager)을 계속 쓸 수 있어야 한다.
     */
    private List<Object> addBookmarksConcurrently(BookmarkRepository racingRepository) throws Exception {
        BookmarkService racing = new BookmarkServiceImpl(racingRepository, userRepository, postRepository);
        return runConcurrently(THREADS, () -> inRequest(() -> {
            racing.addBookmark(details(member), postId);
            return postRepository.findById(postId, member.getId()).orElseThrow(IllegalStateException::new);
        }));
    }

    private void bookmark(User user, long postId) throws Exception {
        inRequest(() -> {
            bookmarkService.addBookmark(details(user), postId);
            return null;
        });
    }

    private void unbookmark(User user, long postId) throws Exception {
        inRequest(() -> {
            bookmarkService.removeBookmark(details(user), postId);
            return null;
        });
    }

    private void block(User user, long blockedUserId) throws Exception {
        inRequest(() -> {
            userService.blockUser(user, blockedUserId);
            return null;
        });
    }

    private void unblock(User user, long blockedUserId) throws Exception {
        inRequest(() -> {
            userService.cancelBlockUser(user, blockedUserId);
            return null;
        });
    }

    /**
     * 운영의 open-in-view(OpenEntityManagerInViewInterceptor)처럼 요청 스레드에 EntityManager를 묶고 task를 실행한다.
     * 북마크 경로는 PostEntity.toModel()이 user를 지연 로딩하므로 반드시 이 안에서 호출한다(밖이면 LazyInitializationException).
     * 차단 경로는 운영처럼 요청 EntityManager를 공유하는 상황(진 요청이 롤백되면 clear됨)을 재현하려고 이 안에서 호출한다.
     * 중첩 호출은 지원하지 않는다.
     */
    private <T> T inRequest(Callable<T> task) throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try {
            return task.call();
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            EntityManagerFactoryUtils.closeEntityManager(entityManager);
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
            // 멈춘 스레드가 커넥션을 쥔 채 다음 테스트의 TRUNCATE와 겹치지 않게 끝날 때까지 기다린다.
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static List<Object> failures(List<Object> results) {
        return results.stream().filter(r -> r instanceof Throwable).collect(Collectors.toList());
    }

    private List<String> uniqueIndexColumns(String table, String index) {
        return jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? AND NON_UNIQUE = 0 "
                        + "ORDER BY SEQ_IN_INDEX",
                String.class, table, index);
    }

    @FunctionalInterface
    private interface Body {
        void run() throws Exception;
    }

    /**
     * unique 인덱스를 잠시 없애 운영 DB의 마이그레이션 전 상태(중복 행 존재)를 만든다.
     * unique 인덱스가 user_id 외래 키의 인덱스를 대신하고 있으므로, user_id 인덱스를 먼저 만든 뒤 지운다(오류 1553 방지).
     * 끝나면 행을 비우고 인덱스를 되돌린다. 본문이 실패하면 되돌리기 실패는 원래 실패에 덧붙인다.
     */
    private void withoutUniqueIndex(String table, String uniqueIndex, String uniqueColumns, Body body) throws Exception {
        String fallbackIndex = "tmp_" + table + "_user_id";
        jdbc.execute("ALTER TABLE " + table + " ADD INDEX " + fallbackIndex + " (user_id), DROP INDEX " + uniqueIndex);
        try {
            body.run();
        } catch (Exception | Error e) {
            try {
                restoreUniqueIndex(table, uniqueIndex, uniqueColumns, fallbackIndex);
            } catch (RuntimeException restoreFailure) {
                e.addSuppressed(restoreFailure);
            }
            throw e;
        }
        restoreUniqueIndex(table, uniqueIndex, uniqueColumns, fallbackIndex);
    }

    private void restoreUniqueIndex(String table, String uniqueIndex, String uniqueColumns, String fallbackIndex) {
        jdbc.execute("DELETE FROM " + table);
        jdbc.execute("ALTER TABLE " + table + " ADD UNIQUE KEY " + uniqueIndex + " (" + uniqueColumns + "), "
                + "DROP INDEX " + fallbackIndex);
    }

    private void insertBookmarkRow(long userId, long postId) {
        jdbc.update("INSERT INTO post_bookmarks (user_id, post_id, deleted) VALUES (?, ?, 0)", userId, postId);
    }

    private void insertBlockRow(long userId, long blockedUserId) {
        jdbc.update("INSERT INTO user_blocks (user_id, blocked_user_id, deleted) VALUES (?, ?, 0)",
                userId, blockedUserId);
    }

    private List<Long> ids(String table) {
        return jdbc.queryForList("SELECT id FROM " + table + " ORDER BY id", Long.class);
    }

    private static String script(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    /**
     * 테이블 이름만 mig_*로 바꾼다.
     * backup_124_post_bookmarks, uk_post_bookmarks_user_post처럼 앞에 '_'가 붙은 이름은 단어 경계가 아니라서 그대로 남는다.
     */
    private static String onMigrationTables(String script) {
        return script.replaceAll("\\bpost_bookmarks\\b", "mig_post_bookmarks")
                .replaceAll("\\buser_blocks\\b", "mig_user_blocks");
    }

    private void runScript(String script) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try {
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            } finally {
                // 스크립트가 SQL로 바꾼 격리 수준을 되돌린다. 중간에 실패해도 풀로 돌아가는 커넥션에 남지 않게 한다.
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ");
                }
            }
            return null;
        });
    }

    private void dropMigrationTables() {
        jdbc.execute("DROP TABLE IF EXISTS backup_124_post_bookmarks");
        jdbc.execute("DROP TABLE IF EXISTS backup_124_user_blocks");
        jdbc.execute("DROP TABLE IF EXISTS mig_post_bookmarks");
        jdbc.execute("DROP TABLE IF EXISTS mig_user_blocks");
    }
}
