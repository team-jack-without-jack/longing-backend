package com.longing.longing.common.writeguard;

import com.longing.longing.common.ErrorCode;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

public final class IdempotencyKey {

    public static final String HEADER = "Idempotency-Key";

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final String value;

    private IdempotencyKey(String value) {
        this.value = value;
    }

    public static Optional<IdempotencyKey> parse(@Nullable String header) {
        if (header == null) {
            return Optional.empty();
        }
        if (!FORMAT.matcher(header).matches()) {
            throw new WriteGuardException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        return Optional.of(new IdempotencyKey(header));
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
