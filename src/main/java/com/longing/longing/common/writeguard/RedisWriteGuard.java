package com.longing.longing.common.writeguard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
public class RedisWriteGuard implements WriteGuard, AutoCloseable {

    private static final RedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0",
            Long.class);

    private static final RedisScript<Long> RETAIN = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('PEXPIRE', KEYS[1], ARGV[2]) end return 0",
            Long.class);

    private final LettuceConnectionFactory factory;
    private final StringRedisTemplate redis;

    public RedisWriteGuard(LettuceConnectionFactory factory) {
        this.factory = factory;
        this.redis = new StringRedisTemplate(factory);
    }

    @Override
    public Acquisition tryAcquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, token, ttl);
            if (acquired == null) {
                log.warn("write-guard: no reply for SET NX");
                return Acquisition.unavailable();
            }
            return acquired ? Acquisition.acquired(token) : Acquisition.held();
        } catch (RuntimeException e) {
            log.warn("write-guard: Redis unavailable on acquire ({})", e.toString());
            return Acquisition.unavailable();
        }
    }

    @Override
    public void release(String key, String token) {
        try {
            redis.execute(RELEASE, Collections.singletonList(key), token);
        } catch (RuntimeException e) {
            log.warn("write-guard: Redis unavailable on release, key expires by TTL ({})", e.toString());
        }
    }

    @Override
    public void retain(String key, String token, Duration ttl) {
        try {
            redis.execute(RETAIN, Collections.singletonList(key), token, Long.toString(ttl.toMillis()));
        } catch (RuntimeException e) {
            log.warn("write-guard: Redis unavailable on retain, key keeps its TTL ({})", e.toString());
        }
    }

    @Override
    public void close() {
        factory.destroy();
    }
}
