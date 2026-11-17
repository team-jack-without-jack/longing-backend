package com.longing.longing.common.writeguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WriteGuardConfigurationTest {

    @Test
    void rejectsClusterRedis() {
        RedisProperties redis = new RedisProperties();
        RedisProperties.Cluster cluster = new RedisProperties.Cluster();
        cluster.setNodes(Collections.singletonList("localhost:6379"));
        redis.setCluster(cluster);

        assertThatThrownBy(() -> new WriteGuardConfiguration().writeGuard(redis, new WriteGuardProperties()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standalone");
    }

    @Test
    void rejectsInvalidPropertiesBeforeConnecting() {
        WriteGuardProperties properties = new WriteGuardProperties();
        properties.setMaxConnections(0);

        assertThatThrownBy(() -> new WriteGuardConfiguration().writeGuard(new RedisProperties(), properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-connections");
    }

    @Test
    void urlOverridesHostAndPortButDatabaseComesFromProperties() {
        RedisProperties redis = new RedisProperties();
        redis.setHost("ignored");
        redis.setDatabase(3);
        redis.setUrl("redis://example.internal:6390/2");

        LettuceConnectionFactory factory = WriteGuardConfiguration.connectionFactory(redis, new WriteGuardProperties());

        assertThat(factory.getStandaloneConfiguration().getHostName()).isEqualTo("example.internal");
        assertThat(factory.getStandaloneConfiguration().getPort()).isEqualTo(6390);
        assertThat(factory.getStandaloneConfiguration().getDatabase()).isEqualTo(3);
        assertThat(factory.getShareNativeConnection()).isFalse();
    }

    @Test
    void sslPropertyIsKeptWhenUrlIsPlain() {
        RedisProperties redis = new RedisProperties();
        redis.setSsl(true);
        redis.setUrl("redis://example.internal:6390");

        LettuceConnectionFactory factory = WriteGuardConfiguration.connectionFactory(redis, new WriteGuardProperties());

        assertThat(factory.isUseSsl()).isTrue();
    }

    @Test
    void redissUrlEnablesSslAndCredentials() {
        RedisProperties redis = new RedisProperties();
        redis.setUrl("rediss://user:pw@example.internal:6390");

        LettuceConnectionFactory factory = WriteGuardConfiguration.connectionFactory(redis, new WriteGuardProperties());

        assertThat(factory.isUseSsl()).isTrue();
        assertThat(factory.getStandaloneConfiguration().getUsername()).isEqualTo("user");
        assertThat(factory.getStandaloneConfiguration().getPassword()).isEqualTo(RedisPassword.of("pw"));
    }

    @Test
    void poolValidatesOnBorrowAndNamesTheClient() {
        LettuceConnectionFactory factory =
                WriteGuardConfiguration.connectionFactory(new RedisProperties(), new WriteGuardProperties());

        assertThat(((LettucePoolingClientConfiguration) factory.getClientConfiguration()).getPoolConfig().getTestOnBorrow())
                .isTrue();
        assertThat(factory.getClientConfiguration().getClientName()).contains("write-guard");
    }
}
