package com.longing.longing.support;

import com.longing.longing.common.writeguard.Acquisition;
import com.longing.longing.common.writeguard.RedisWriteGuard;
import com.longing.longing.common.writeguard.WriteGuard;
import com.longing.longing.common.writeguard.WriteGuardConfiguration;
import com.longing.longing.common.writeguard.WriteGuardProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public final class RedisTestSupport {

    /** Redis 장애 상황: 모든 잠금 시도가 UNAVAILABLE. */
    public static final WriteGuard UNAVAILABLE = new WriteGuard() {
        @Override
        public Acquisition tryAcquire(String key, Duration ttl) {
            return Acquisition.unavailable();
        }

        @Override
        public void release(String key, String token) {
        }

        @Override
        public void retain(String key, String token, Duration ttl) {
        }
    };

    private RedisTestSupport() {
    }

    public static GenericContainer<?> startRedis() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                .withExposedPorts(6379);
        redis.start();
        return redis;
    }

    public static void flush(GenericContainer<?> redis) throws Exception {
        redis.execInContainer("redis-cli", "FLUSHDB");
    }

    public static RedisWriteGuard guard(GenericContainer<?> redis) {
        return guard(redis, new WriteGuardProperties());
    }

    /** 타임아웃을 다루지 않는 테스트용: 콜드 스타트 지연이 UNAVAILABLE로 바뀌지 않게 넉넉히 둔다. */
    public static WriteGuardProperties patientProperties() {
        WriteGuardProperties properties = new WriteGuardProperties();
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setCommandTimeout(Duration.ofSeconds(2));
        properties.setAcquireTimeout(Duration.ofSeconds(2));
        return properties;
    }

    public static RedisWriteGuard patientGuard(GenericContainer<?> redis) {
        return guard(redis, patientProperties());
    }

    public static RedisWriteGuard guard(GenericContainer<?> redis, WriteGuardProperties properties) {
        return guard(redis.getHost(), redis.getMappedPort(6379), properties);
    }

    public static RedisWriteGuard guard(String host, int port) {
        return guard(host, port, new WriteGuardProperties());
    }

    private static RedisWriteGuard guard(String host, int port, WriteGuardProperties properties) {
        RedisProperties connection = new RedisProperties();
        connection.setHost(host);
        connection.setPort(port);
        return new WriteGuardConfiguration().writeGuard(connection, properties);
    }
}
