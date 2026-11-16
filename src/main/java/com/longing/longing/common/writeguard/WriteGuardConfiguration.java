package com.longing.longing.common.writeguard;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

/**
 * 생성 중복 방지 전용 Redis 연결. Spring 기본 연결(명령 타임아웃 사실상 60초)과 분리해
 * Redis가 멈춰도 짧은 시간 안에 UNAVAILABLE로 판정하고 생성을 계속한다. (#119 rate limiter와 같은 방식)
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({WriteGuardProperties.class, RedisProperties.class})
public class WriteGuardConfiguration {

    @Bean(destroyMethod = "close")
    public RedisWriteGuard writeGuard(RedisProperties redis, WriteGuardProperties properties) {
        properties.validate();
        LettuceConnectionFactory factory = connectionFactory(redis, properties);
        try {
            factory.afterPropertiesSet();
            return new RedisWriteGuard(factory);
        } catch (RuntimeException e) {
            factory.destroy();
            throw e;
        }
    }

    static LettuceConnectionFactory connectionFactory(RedisProperties redis, WriteGuardProperties properties) {
        if (redis.getCluster() != null || redis.getSentinel() != null) {
            throw new IllegalArgumentException("write-guard currently requires standalone Redis");
        }
        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(redis.getHost(), redis.getPort());
        server.setDatabase(redis.getDatabase());
        server.setUsername(redis.getUsername());
        server.setPassword(redis.getPassword());
        boolean ssl = redis.isSsl();
        if (redis.getUrl() != null && !redis.getUrl().isEmpty()) {
            // Spring Boot 2.7과 같은 규칙: url은 호스트·포트·계정을 덮어쓰고 SSL은 켤 수만 있다. database는 spring.redis.database를 쓴다.
            RedisURI uri = RedisURI.create(redis.getUrl());
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new IllegalArgumentException("spring.redis.url must name a Redis host");
            }
            server.setHostName(uri.getHost());
            server.setPort(uri.getPort());
            server.setUsername(uri.getUsername());
            server.setPassword(RedisPassword.of(uri.getPassword()));
            ssl = ssl || uri.isSsl();
        }
        GenericObjectPoolConfig<StatefulConnection<?, ?>> pool = new GenericObjectPoolConfig<>();
        pool.setMaxTotal(properties.getMaxConnections());
        pool.setMaxIdle(properties.getMaxConnections());
        pool.setMinIdle(0);
        pool.setMaxWait(properties.getAcquireTimeout());
        // 끊긴 연결을 빌려주지 않는다(로컬 isOpen 확인만 하므로 네트워크 왕복이 없다). 없으면 Redis 복구 후에도 최대 30초간 UNAVAILABLE.
        pool.setTestOnBorrow(true);
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder client =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(pool)
                        .clientName("write-guard")
                        .commandTimeout(properties.getCommandTimeout())
                        .shutdownTimeout(Duration.ofMillis(100))
                        .clientOptions(ClientOptions.builder()
                                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                                .socketOptions(SocketOptions.builder()
                                        .connectTimeout(properties.getConnectTimeout())
                                        .build())
                                .build());
        if (ssl) {
            client.useSsl();
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(server, client.build());
        // 공유 연결 초기화는 실패한 접속을 제한 없는 모니터에서 직렬화한다. 풀 연결로 대기 시간을 제한한다.
        factory.setShareNativeConnection(false);
        return factory;
    }
}
