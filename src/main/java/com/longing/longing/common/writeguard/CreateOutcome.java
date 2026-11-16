package com.longing.longing.common.writeguard;

import lombok.Getter;

@Getter
public final class CreateOutcome<T> {

    private final T value;
    private final boolean replayed;

    private CreateOutcome(T value, boolean replayed) {
        this.value = value;
        this.replayed = replayed;
    }

    public static <T> CreateOutcome<T> created(T value) {
        return new CreateOutcome<>(value, false);
    }

    public static <T> CreateOutcome<T> replayed(T value) {
        return new CreateOutcome<>(value, true);
    }
}
