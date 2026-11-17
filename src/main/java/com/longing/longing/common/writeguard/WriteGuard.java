package com.longing.longing.common.writeguard;

import java.time.Duration;

public interface WriteGuard {

    /** {@code SET key token NX PX ttl}. Redis 장애는 예외 대신 {@link Acquisition#unavailable()}로 반환한다. */
    Acquisition tryAcquire(String key, Duration ttl);

    /** key가 아직 token을 가지고 있을 때만 삭제한다. Redis 장애는 예외 없이 무시하고 TTL 만료에 맡긴다. */
    void release(String key, String token);

    /** key가 아직 token을 가지고 있을 때만 남은 유지 시간을 ttl로 바꾼다. Redis 장애는 예외 없이 무시한다(기존 TTL로 만료). */
    void retain(String key, String token, Duration ttl);
}
