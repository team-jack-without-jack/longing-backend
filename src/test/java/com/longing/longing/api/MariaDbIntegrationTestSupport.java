package com.longing.longing.api;

import com.longing.longing.api.comment.controller.port.CommentService;
import com.longing.longing.api.comment.infrastructure.CommentRepositoryImpl;
import com.longing.longing.api.comment.service.CommentServiceImpl;
import com.longing.longing.api.comment.service.port.CommentRepository;
import com.longing.longing.api.post.controller.port.PostService;
import com.longing.longing.api.post.infrastructure.PostEntity;
import com.longing.longing.api.post.infrastructure.PostJpaRepository;
import com.longing.longing.api.post.infrastructure.PostRepositoryImpl;
import com.longing.longing.api.post.service.PostServiceImpl;
import com.longing.longing.api.post.service.port.PostRepository;
import com.longing.longing.api.report.ReportReason;
import com.longing.longing.api.report.controller.port.ReportService;
import com.longing.longing.api.report.domain.ReportCreate;
import com.longing.longing.api.report.infrastructure.ReportRepositoryImpl;
import com.longing.longing.api.report.service.ReportServiceImpl;
import com.longing.longing.api.report.service.port.ReportRepository;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.Role;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.infrastructure.UserEntity;
import com.longing.longing.api.user.infrastructure.UserJpaRepository;
import com.longing.longing.api.user.infrastructure.UserRepositoryImpl;
import com.longing.longing.api.user.service.port.UserRepository;
import com.longing.longing.bookmark.infrastructure.BookmarkRepositoryImpl;
import com.longing.longing.bookmark.service.port.BookmarkRepository;
import com.longing.longing.common.infrastructure.PostImageRepositoryImpl;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.common.service.port.PostImageRepository;
import com.longing.longing.config.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 MariaDB 10.6(Testcontainers) 통합 테스트의 공통 도구 (#126, #129, #128).
 * 컨테이너와 Spring 컨텍스트를 하위 클래스들이 함께 쓴다. 설정이 같아서 컨텍스트가 캐시된다.
 * 하위 클래스는 컨텍스트에 영향을 주는 어노테이션(@MockBean, @Import 등)을 더하지 않는다. 더하면 컨텍스트가 하나 더 뜬다.
 * {@code member}·{@code postId}는 {@link #setUpDatabase()}가 테스트마다 채운다. 하위 클래스에서 같은 이름으로 다시 선언하지 않는다.
 */
// 동시성 테스트는 최대 THREADS(8)개 스레드가 각자 커넥션을 쥔 채 게시글 잠금을 기다릴 수 있다.
// 스레드 수를 늘리면 maximum-pool-size도 함께 늘린다.
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MariaDB106Dialect",
        "spring.datasource.hikari.maximum-pool-size=12"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({QueryDslConfig.class, PostRepositoryImpl.class, ReportRepositoryImpl.class, CommentRepositoryImpl.class,
        UserRepositoryImpl.class, PostImageRepositoryImpl.class, BookmarkRepositoryImpl.class})
abstract class MariaDbIntegrationTestSupport {
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.6"));

    static final int THREADS = 8;
    /** 확인 단계 뒤에 멈추는 시간. 그 사이 다른 요청이 같은 확인을 통과할 틈을 만든다. */
    static final long PAUSE_MS = 300;

    static {
        MARIADB.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        // 잠금 회귀가 생기면 50초가 아니라 10초 만에 실패하게 한다.
        registry.add("spring.datasource.url", () -> withSessionVariable(MARIADB.getJdbcUrl(), "innodb_lock_wait_timeout=10"));
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
    }

    @MockBean S3ImageService s3ImageService;

    @Autowired ReportRepository reportRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostImageRepository postImageRepository;
    @Autowired UserRepository userRepository;
    @Autowired PostJpaRepository postJpaRepository;
    @Autowired UserJpaRepository userJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired DataSource dataSource;
    @Autowired BookmarkRepository bookmarkRepository;

    User member;
    long postId;

    @BeforeEach
    void setUpDatabase() {
        truncate("post_reports", "comments", "post_images", "post_likes", "post_bookmarks", "posts", "users");
        // 게시글 id가 사용자 id와 겹치지 않게 해서 인자 순서 실수를 테스트가 잡게 한다.
        jdbc.execute("ALTER TABLE posts AUTO_INCREMENT = 1001");
        member = saveUser("member@example.com", "member-1");
        postId = seedPost(member);
    }

    ReportService reportService(ReportRepository reports) {
        return ReportServiceImpl.builder().reportRepository(reports).postRepository(postRepository).build();
    }

    CommentService commentService(CommentRepository comments) {
        return CommentServiceImpl.builder()
                .commentRepository(comments)
                .userRepository(userRepository)
                .postJpaRepository(postJpaRepository)
                .postRepository(postRepository)
                .build();
    }

    PostService postService(PostRepository posts) {
        return PostServiceImpl.builder()
                .postRepository(posts)
                .s3ImageService(s3ImageService)
                .postImageRepository(postImageRepository)
                .build();
    }

    static ReportCreate report(ReportReason reason) {
        return ReportCreate.builder().reportReason(reason).build();
    }

    User saveUser(String email, String providerId) {
        return userJpaRepository.save(UserEntity.builder()
                .name(email).email(email)
                .provider(Provider.GOOGLE).providerId(providerId).role(Role.GUEST)
                .build()).toModel();
    }

    long seedPost(User owner) {
        return postJpaRepository.save(PostEntity.builder()
                .title("title").content("content").user(UserEntity.fromModel(owner))
                .build()).getId();
    }

    long seedComment(long targetPostId, User author, boolean deleted) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO comments (content, post_id, user_id, deleted) VALUES ('hello', ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, targetPostId);
            statement.setLong(2, author.getId());
            statement.setBoolean(3, deleted);
            return statement;
        }, key);
        if (!deleted) {
            jdbc.update("UPDATE posts SET comment_count = comment_count + 1 WHERE id = ?", targetPostId);
        }
        return key.getKey().longValue();
    }

    void seedImage(String address) {
        jdbc.update("INSERT INTO post_images (address, post_id, user_id, deleted) VALUES (?, ?, ?, 0)",
                address, postId, member.getId());
    }

    void truncate(String... tables) {
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

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    boolean postDeleted() {
        return jdbc.queryForObject("SELECT deleted FROM posts WHERE id = ?", Boolean.class, postId);
    }

    int commentCountOf(long targetPostId) {
        return jdbc.queryForObject("SELECT comment_count FROM posts WHERE id = ?", Integer.class, targetPostId);
    }

    boolean commentDeleted(long commentId) {
        return jdbc.queryForObject("SELECT deleted FROM comments WHERE id = ?", Boolean.class, commentId);
    }

    long activeImages() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post_images WHERE post_id = ? AND deleted = 0",
                Long.class, postId);
    }

    /**
     * 다른 요청의 쓰기를 흉내 낸다. 별도 트랜잭션(REQUIRES_NEW)으로 바로 커밋한다.
     * inTransaction 안의 훅에서 그냥 jdbc를 쓰면 바깥 트랜잭션에 묶여, 예외로 롤백될 때 함께 사라진다.
     * 같은 스레드에서 실행하므로, 바깥 트랜잭션이 이미 잡은 행 잠금이 필요한 쓰기에는 쓸 수 없다(자기 자신을 기다리다 잠금 대기 시간 초과).
     * 그런 경우는 다른 스레드에서 실행하고 awaitLockWait로 기다린다.
     */
    void asAnotherRequest(Runnable writes) {
        TransactionTemplate other = new TransactionTemplate(transactionManager);
        other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        other.executeWithoutResult(status -> writes.run());
    }

    /** 서비스의 @Transactional과 같은 트랜잭션(REQUIRED) 안에서 실행한다. 직접 만든 서비스에는 트랜잭션 프록시가 없다. */
    <T> T inTransaction(Supplier<T> task) {
        return new TransactionTemplate(transactionManager).execute(status -> task.get());
    }

    /**
     * 운영의 open-in-view(OpenEntityManagerInViewInterceptor)처럼 요청 스레드에 EntityManager를 묶고 task를 실행한다.
     * 게시글 경로는 PostEntity.toModel()이 user를 지연 로딩하므로 이 안에서 호출해야 한다. 중첩 호출은 지원하지 않는다.
     */
    <T> T inRequest(Callable<T> task) throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try {
            return task.call();
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            EntityManagerFactoryUtils.closeEntityManager(entityManager);
        }
    }

    /**
     * checkMethod가 끝날 때마다 PAUSE_MS만큼 멈춘다. 확인과 쓰기 사이에 다른 요청이 끼어들 틈을 만든다.
     * 호출 수를 checks에 세어서, 테스트가 경쟁이 실제로 일어났는지 확인할 수 있게 한다.
     */
    static <S> S pausingAfter(Class<S> type, S target, String checkMethod, AtomicInteger checks) {
        requireMethod(type, checkMethod);
        InvocationHandler handler = (proxy, method, args) -> {
            Object result = invoke(target, method, args);
            if (method.getName().equals(checkMethod)) {
                checks.incrementAndGet();
                Thread.sleep(PAUSE_MS);
            }
            return result;
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    /** methodName이 처음 시작되기 직전에 action을 한 번 실행하고 ran을 true로 바꾼다. 쓰기 직전에 다른 요청이 끼어든 상황을 만든다. */
    static <S> S beforeFirstCall(Class<S> type, S target, String methodName, AtomicBoolean ran, Runnable action) {
        requireMethod(type, methodName);
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals(methodName) && ran.compareAndSet(false, true)) {
                action.run();
            }
            return invoke(target, method, args);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    /** methodName이 처음 끝난 직후에 action을 한 번 실행하고 ran을 true로 바꾼다. 읽은 직후 다른 요청이 끼어든 상황을 만든다. */
    static <S> S afterFirstCall(Class<S> type, S target, String methodName, AtomicBoolean ran, Runnable action) {
        requireMethod(type, methodName);
        InvocationHandler handler = (proxy, method, args) -> {
            Object result = invoke(target, method, args);
            if (method.getName().equals(methodName) && ran.compareAndSet(false, true)) {
                action.run();
            }
            return result;
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** 이름 오타로 프록시가 조용히 아무 일도 안 하는 일을 막는다. */
    static void requireMethod(Class<?> type, String name) {
        if (Arrays.stream(type.getMethods()).noneMatch(m -> m.getName().equals(name))) {
            throw new IllegalArgumentException(type.getSimpleName() + " has no method " + name);
        }
    }

    static List<Object> runConcurrently(int threads, Callable<?> task) throws Exception {
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
                results.add(future.get(90, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
            // 멈춘 스레드가 커넥션을 쥔 채 다음 테스트의 TRUNCATE와 겹치지 않게 끝날 때까지 기다린다.
            // finally에서 단언하면 원래 실패를 가리므로 기록만 한다.
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("runConcurrently: worker threads still running after 10s");
            }
        }
    }

    static String withSessionVariable(String jdbcUrl, String variable) {
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sessionVariables=" + variable;
    }

    static List<Object> failures(List<Object> results) {
        return results.stream().filter(r -> r instanceof Throwable).collect(Collectors.toList());
    }

    /**
     * 실행 중인 문장에 queryFragment가 들어 있는 다른 트랜잭션이 잠금을 기다리는 상태가 될 때까지 기다린다.
     * INNODB_TRX는 PROCESS 권한이 필요해서 root로 본다.
     * INNODB_TRX는 캐시를 읽고, 이 캐시는 마지막으로 읽힌 뒤 100ms가 지나야 새로 채워진다(CACHE_MIN_IDLE_TIME_US).
     * 그보다 자주 읽으면 첫 조회 시점의 내용에 묶여 그 뒤의 잠금 대기를 끝내 못 본다. 그래서 200ms 간격으로 본다.
     */
    void awaitLockWait(String queryFragment) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);   // 잠금 대기 시간 초과(10초)보다 짧게
        // Testcontainers MariaDB는 root 비밀번호를 앱 사용자 비밀번호와 같게 둔다.
        try (Connection root = DriverManager.getConnection(MARIADB.getJdbcUrl(), "root", MARIADB.getPassword());
             PreparedStatement statement = root.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.INNODB_TRX WHERE trx_state = 'LOCK WAIT' "
                             + "AND trx_query LIKE ?")) {
            statement.setString(1, "%" + queryFragment + "%");
            while (true) {
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    if (rows.getLong(1) > 0) {
                        return;
                    }
                }
                if (System.nanoTime() > deadline) {
                    throw new IllegalStateException("no transaction started waiting for a lock: " + queryFragment);
                }
                Thread.sleep(200);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
