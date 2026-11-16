package com.longing.longing.common.writeguard;

import java.util.Objects;

public final class Acquisition {

    private enum Status { ACQUIRED, HELD, UNAVAILABLE }

    private static final Acquisition HELD = new Acquisition(Status.HELD, null);
    private static final Acquisition UNAVAILABLE = new Acquisition(Status.UNAVAILABLE, null);

    private final Status status;
    private final String token;

    private Acquisition(Status status, String token) {
        this.status = status;
        this.token = token;
    }

    public static Acquisition acquired(String token) {
        return new Acquisition(Status.ACQUIRED, Objects.requireNonNull(token, "token"));
    }

    public static Acquisition held() {
        return HELD;
    }

    public static Acquisition unavailable() {
        return UNAVAILABLE;
    }

    public boolean isAcquired() {
        return status == Status.ACQUIRED;
    }

    public boolean isHeld() {
        return status == Status.HELD;
    }

    public boolean isUnavailable() {
        return status == Status.UNAVAILABLE;
    }

    public String token() {
        if (!isAcquired()) {
            throw new IllegalStateException("No token for " + status);
        }
        return token;
    }
}
