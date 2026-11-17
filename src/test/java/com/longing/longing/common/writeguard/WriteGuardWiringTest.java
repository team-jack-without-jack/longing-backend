package com.longing.longing.common.writeguard;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** 전체 애플리케이션 컨텍스트는 테스트에서 뜨지 않으므로, 가드 관련 빈만 실제 Spring 설정으로 조립해 검증한다. */
class WriteGuardWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
            .withUserConfiguration(WriteGuardConfiguration.class, CreateGuardExecutor.class, Metrics.class)
            .withPropertyValues("spring.redis.host=127.0.0.1", "spring.redis.port=" + freePort());

    @Test
    void startsWithoutRedisAndFailsOpen() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WriteGuard.class);
            assertThat(context).hasSingleBean(RedisConnectionFactory.class);
            assertThat(context).hasSingleBean(WriteGuardProperties.class);

            CreateRequest<String> request = CreateRequest.<String>builder()
                    .scope("post")
                    .userId(1L)
                    .key(Optional.empty())
                    .fingerprint("f")
                    .uniqueConstraint("uk")
                    .findByKey(k -> Optional.empty())
                    .sameContent(s -> true)
                    .create(k -> "created")
                    .build();

            long start = System.nanoTime();
            CreateOutcome<String> outcome = context.getBean(CreateGuardExecutor.class).execute(request);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

            assertThat(outcome.getValue()).isEqualTo("created");
            assertThat(outcome.isReplayed()).isFalse();
            assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
        });
    }

    @Test
    void rejectsInvalidWriteGuardPropertiesAtStartup() {
        runner.withPropertyValues("write-guard.max-connections=0").run(context -> {
            assertThat(context).hasFailed();
            Throwable root = context.getStartupFailure();
            while (root.getCause() != null) {
                root = root.getCause();
            }
            assertThat(root.getMessage()).contains("write-guard.max-connections");
        });
    }

    @Test
    void bindsWriteGuardPropertiesFromConfiguration() {
        runner.withPropertyValues("write-guard.dedupe-window=3s", "write-guard.in-flight-ttl=45s").run(context -> {
            WriteGuardProperties properties = context.getBean(WriteGuardProperties.class);
            assertThat(properties.getDedupeWindow()).isEqualTo(Duration.ofSeconds(3));
            assertThat(properties.getInFlightTtl()).isEqualTo(Duration.ofSeconds(45));
        });
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Metrics {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
