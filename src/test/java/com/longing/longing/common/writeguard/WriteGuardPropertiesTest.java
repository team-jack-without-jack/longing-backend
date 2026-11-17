package com.longing.longing.common.writeguard;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WriteGuardPropertiesTest {

    @Test
    void defaultsAreValid() {
        WriteGuardProperties properties = new WriteGuardProperties();
        assertThatCode(properties::validate).doesNotThrowAnyException();
        assertThat(properties.getDedupeWindow()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getInFlightTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getCommandTimeout()).isEqualTo(Duration.ofMillis(200));
        assertThat(properties.getMaxConnections()).isEqualTo(16);
    }

    @Test
    void rejectsOutOfRangeValuesNamingTheProperty() {
        assertInvalid(p -> p.setDedupeWindow(Duration.ofMillis(99)), "dedupe-window");
        assertInvalid(p -> p.setDedupeWindow(Duration.ofMinutes(2)), "dedupe-window");
        assertInvalid(p -> p.setInFlightTtl(Duration.ofMillis(999)), "in-flight-ttl");
        assertInvalid(p -> p.setInFlightTtl(Duration.ofMinutes(11)), "in-flight-ttl");
        assertInvalid(p -> {
            p.setInFlightTtl(Duration.ofSeconds(1));
            p.setDedupeWindow(Duration.ofSeconds(5));
        }, "in-flight-ttl");
        assertInvalid(p -> p.setConnectTimeout(Duration.ofSeconds(6)), "connect-timeout");
        assertInvalid(p -> p.setCommandTimeout(Duration.ZERO), "command-timeout");
        assertInvalid(p -> p.setAcquireTimeout(null), "acquire-timeout");
        assertInvalid(p -> p.setMaxConnections(0), "max-connections");
        assertInvalid(p -> p.setMaxConnections(129), "max-connections");
    }

    @Test
    void acceptsExactBounds() {
        for (Consumer<WriteGuardProperties> change : Arrays.<Consumer<WriteGuardProperties>>asList(
                p -> p.setDedupeWindow(Duration.ofMillis(100)),
                p -> {
                    p.setDedupeWindow(Duration.ofMinutes(1));
                    p.setInFlightTtl(Duration.ofMinutes(1));
                },
                p -> {
                    p.setInFlightTtl(Duration.ofSeconds(1));
                    p.setDedupeWindow(Duration.ofMillis(100));
                },
                p -> p.setInFlightTtl(Duration.ofMinutes(10)),
                p -> p.setCommandTimeout(Duration.ofMillis(1)),
                p -> p.setConnectTimeout(Duration.ofSeconds(5)),
                p -> p.setMaxConnections(1),
                p -> p.setMaxConnections(128))) {
            WriteGuardProperties properties = new WriteGuardProperties();
            change.accept(properties);
            assertThatCode(properties::validate).doesNotThrowAnyException();
        }
    }

    private static void assertInvalid(Consumer<WriteGuardProperties> change, String property) {
        WriteGuardProperties properties = new WriteGuardProperties();
        change.accept(properties);
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("write-guard." + property);
    }
}
