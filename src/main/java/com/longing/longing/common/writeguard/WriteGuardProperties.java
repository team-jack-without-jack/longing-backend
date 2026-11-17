package com.longing.longing.common.writeguard;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "write-guard")
public class WriteGuardProperties {

    /** 키 없는 요청: 생성이 성공한 시점부터 같은 내용 재요청을 차단하는 시간. */
    private Duration dedupeWindow = Duration.ofSeconds(5);
    /** 처리 중 잠금·지문 유지 시간(키 유무 공통). 생성 최대 소요 시간보다 길어야 한다. */
    private Duration inFlightTtl = Duration.ofSeconds(30);
    private Duration connectTimeout = Duration.ofMillis(200);
    private Duration commandTimeout = Duration.ofMillis(200);
    private Duration acquireTimeout = Duration.ofMillis(200);
    private int maxConnections = 16;

    public void validate() {
        requireBetween(dedupeWindow, Duration.ofMillis(100), Duration.ofMinutes(1), "dedupe-window");
        requireBetween(inFlightTtl, Duration.ofSeconds(1), Duration.ofMinutes(10), "in-flight-ttl");
        requireBetween(connectTimeout, Duration.ofMillis(1), Duration.ofSeconds(5), "connect-timeout");
        requireBetween(commandTimeout, Duration.ofMillis(1), Duration.ofSeconds(5), "command-timeout");
        requireBetween(acquireTimeout, Duration.ofMillis(1), Duration.ofSeconds(5), "acquire-timeout");
        if (inFlightTtl.compareTo(dedupeWindow) < 0) {
            throw new IllegalArgumentException("write-guard.in-flight-ttl must be >= write-guard.dedupe-window");
        }
        if (maxConnections < 1 || maxConnections > 128) {
            throw new IllegalArgumentException("write-guard.max-connections must be between 1 and 128");
        }
    }

    private static void requireBetween(Duration value, Duration min, Duration max, String name) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException("write-guard." + name + " must be between " + min + " and " + max);
        }
    }
}
