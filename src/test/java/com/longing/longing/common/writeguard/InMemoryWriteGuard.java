package com.longing.longing.common.writeguard;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** TTL은 기록만 하고 만료는 시뮬레이션하지 않는다. */
class InMemoryWriteGuard implements WriteGuard {

    private final Map<String, String> held = new ConcurrentHashMap<>();
    private final Map<String, Duration> ttls = new ConcurrentHashMap<>();
    final List<String> released = new CopyOnWriteArrayList<>();
    volatile boolean unavailable;

    @Override
    public Acquisition tryAcquire(String key, Duration ttl) {
        if (unavailable) {
            return Acquisition.unavailable();
        }
        String token = UUID.randomUUID().toString();
        if (held.putIfAbsent(key, token) != null) {
            return Acquisition.held();
        }
        ttls.put(key, ttl);
        return Acquisition.acquired(token);
    }

    @Override
    public void release(String key, String token) {
        released.add(key);
        if (held.remove(key, token)) {
            ttls.remove(key);
        }
    }

    @Override
    public void retain(String key, String token, Duration ttl) {
        if (token.equals(held.get(key))) {
            ttls.put(key, ttl);
        }
    }

    boolean isHeld(String key) {
        return held.containsKey(key);
    }

    Duration ttl(String key) {
        return ttls.get(key);
    }
}
