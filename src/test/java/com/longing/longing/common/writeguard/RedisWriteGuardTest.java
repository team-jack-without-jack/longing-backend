package com.longing.longing.common.writeguard;

import com.longing.longing.support.RedisTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RedisWriteGuardTest {

    static GenericContainer<?> redis;
    static LettuceConnectionFactory inspectorFactory;
    static StringRedisTemplate inspector;
    RedisWriteGuard guard;

    @BeforeAll
    static void startRedis() {
        redis = RedisTestSupport.startRedis();
        inspectorFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        inspectorFactory.afterPropertiesSet();
        inspector = new StringRedisTemplate(inspectorFactory);
    }

    @AfterAll
    static void stopRedis() {
        if (inspectorFactory != null) {
            inspectorFactory.destroy();
        }
        if (redis != null) {
            redis.stop();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        RedisTestSupport.flush(redis); // also waits out a CLIENT PAUSE from a previous test
        guard = RedisTestSupport.guard(redis);
    }

    @AfterEach
    void tearDown() {
        guard.close();
    }

    @Test
    void sameKeyIsHeldUntilReleasedAndOtherKeysAreIndependent() {
        Acquisition first = guard.tryAcquire("k", Duration.ofSeconds(10));

        assertThat(first.isAcquired()).isTrue();
        assertThat(guard.tryAcquire("k", Duration.ofSeconds(10)).isHeld()).isTrue();
        assertThat(guard.tryAcquire("other", Duration.ofSeconds(10)).isAcquired()).isTrue();

        guard.release("k", first.token());
        assertThat(guard.tryAcquire("k", Duration.ofSeconds(10)).isAcquired()).isTrue();
    }

    @Test
    void keyExpiresAfterTtl() throws Exception {
        assertThat(guard.tryAcquire("k", Duration.ofMillis(300)).isAcquired()).isTrue();
        assertThat(pttl("k")).isBetween(1L, 300L);

        Thread.sleep(400);

        assertThat(guard.tryAcquire("k", Duration.ofSeconds(1)).isAcquired()).isTrue();
    }

    @Test
    void retainChangesTtlOnlyForTheOwner() throws Exception {
        Acquisition owner = guard.tryAcquire("k", Duration.ofSeconds(30));

        guard.retain("k", "not-the-owner", Duration.ofMillis(500));
        assertThat(pttl("k")).isGreaterThan(1_000L);

        guard.retain("k", owner.token(), Duration.ofMillis(500));
        assertThat(pttl("k")).isBetween(1L, 500L);
    }

    @Test
    void releaseWithAnotherTokenKeepsTheKey() {
        Acquisition first = guard.tryAcquire("k", Duration.ofSeconds(10));

        guard.release("k", "not-the-owner");
        assertThat(guard.tryAcquire("k", Duration.ofSeconds(10)).isHeld()).isTrue();

        guard.release("k", first.token());
        assertThat(guard.tryAcquire("k", Duration.ofSeconds(10)).isAcquired()).isTrue();
    }

    @Test
    void unreachableRedisIsUnavailableQuicklyAndReleaseDoesNotThrow() throws IOException {
        try (RedisWriteGuard unreachable = RedisTestSupport.guard("127.0.0.1", freePort())) {
            long started = System.nanoTime();

            assertThat(unreachable.tryAcquire("k", Duration.ofSeconds(1)).isUnavailable()).isTrue();
            assertThatCode(() -> unreachable.release("k", "token")).doesNotThrowAnyException();
            assertThatCode(() -> unreachable.retain("k", "token", Duration.ofSeconds(1))).doesNotThrowAnyException();
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
        }
    }

    @Test
    void pausedRedisIsUnavailableWithinCommandTimeout() throws Exception {
        assertThat(guard.tryAcquire("warm", Duration.ofSeconds(10)).isAcquired()).isTrue();
        redis.execInContainer("redis-cli", "CLIENT", "PAUSE", "1500", "ALL");
        long started = System.nanoTime();

        assertThat(guard.tryAcquire("k", Duration.ofSeconds(10)).isUnavailable()).isTrue();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(1));

        RedisTestSupport.flush(redis); // blocks until the pause ends
        assertThat(guard.tryAcquire("after-pause", Duration.ofSeconds(10)).isAcquired()).isTrue();
    }

    private static long pttl(String key) throws Exception {
        return inspector.getExpire(key, TimeUnit.MILLISECONDS);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
