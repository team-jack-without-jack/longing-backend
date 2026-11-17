package com.longing.longing.common.writeguard;

import com.longing.longing.common.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyTest {

    @Test
    void absentHeaderMeansNoKey() {
        assertThat(IdempotencyKey.parse(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "550e8400-e29b-41d4-a716-446655440000", "AZaz09_-"})
    void acceptsAllowedCharacters(String header) {
        Optional<IdempotencyKey> key = IdempotencyKey.parse(header);
        assertThat(key).isPresent();
        assertThat(key.get().value()).isEqualTo(header);
    }

    @Test
    void acceptsExactly64Characters() {
        String header = "a".repeat(64);
        assertThat(IdempotencyKey.parse(header).map(IdempotencyKey::value)).contains(header);
    }

    @Test
    void rejects65Characters() {
        assertInvalid("a".repeat(65));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "has space", "slash/", "dot.key", "한글키"})
    void rejectsMalformed(String header) {
        assertInvalid(header);
    }

    private static void assertInvalid(String header) {
        assertThatThrownBy(() -> IdempotencyKey.parse(header))
                .isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_IDEMPOTENCY_KEY);
    }
}
