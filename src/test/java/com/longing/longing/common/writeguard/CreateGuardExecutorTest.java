package com.longing.longing.common.writeguard;

import com.longing.longing.common.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateGuardExecutorTest {

    private static final String CONSTRAINT = "uk_items_user_client_request";
    private static final long USER = 7L;

    private InMemoryWriteGuard guard;
    private SimpleMeterRegistry meters;
    private CreateGuardExecutor executor;
    private Map<String, Item> rows;
    private AtomicLong ids;

    @BeforeEach
    void setUp() {
        guard = new InMemoryWriteGuard();
        meters = new SimpleMeterRegistry();
        executor = new CreateGuardExecutor(guard, new WriteGuardProperties(), meters);
        rows = new ConcurrentHashMap<>();
        ids = new AtomicLong();
    }

    // ---- 키 없음: 내용 지문 ----

    @Test
    void keylessFirstSubmissionCreatesAndHoldsFingerprint() {
        CreateOutcome<Item> outcome = executor.execute(request(USER, "hello", null).build());

        assertThat(outcome.isReplayed()).isFalse();
        assertThat(outcome.getValue().content).isEqualTo("hello");
        assertThat(guard.isHeld(dedupeKey(USER, "hello"))).isTrue();
        assertThat(guard.ttl(dedupeKey(USER, "hello"))).isEqualTo(Duration.ofSeconds(5));
        assertThat(decisions("dedupe", "created", "available")).isEqualTo(1.0);
    }

    @Test
    void keylessSameContentWithinWindowIsRejectedWithoutCreating() {
        executor.execute(request(USER, "hello", null).build());

        assertThatThrownBy(() -> executor.execute(request(USER, "hello", null).build()))
                .isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_SUBMISSION);
        assertThat(ids.get()).isEqualTo(1);
        assertThat(decisions("dedupe", "duplicate", "available")).isEqualTo(1.0);
        assertThat(guard.released).isEmpty();
        assertThat(guard.isHeld(dedupeKey(USER, "hello"))).isTrue();
    }

    @Test
    void keylessDifferentContentOrUserIsNotBlocked() {
        executor.execute(request(USER, "hello", null).build());
        executor.execute(request(USER, "bye", null).build());
        executor.execute(request(8L, "hello", null).build());

        assertThat(ids.get()).isEqualTo(3);
    }

    @Test
    void keylessFailedCreateReleasesFingerprintSoImmediateRetryWorks() {
        CreateRequest<Item> failing = request(USER, "hello", null)
                .create(key -> {
                    throw new IllegalStateException("s3 down");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(failing)).isInstanceOf(IllegalStateException.class);
        assertThat(guard.isHeld(dedupeKey(USER, "hello"))).isFalse();
        assertThat(decisions("dedupe", "error", "available")).isEqualTo(1.0);
        assertThat(executor.execute(request(USER, "hello", null).build()).isReplayed()).isFalse();
    }

    @Test
    void keylessErrorFromCreateStillReleasesFingerprint() {
        CreateRequest<Item> failing = request(USER, "hello", null)
                .create(key -> {
                    throw new AssertionError("boom");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(failing)).isInstanceOf(AssertionError.class);
        assertThat(guard.isHeld(dedupeKey(USER, "hello"))).isFalse();
    }

    @Test
    void keylessRedisUnavailableStillCreates() {
        guard.unavailable = true;

        executor.execute(request(USER, "hello", null).build());
        executor.execute(request(USER, "hello", null).build());

        assertThat(ids.get()).isEqualTo(2);
        assertThat(decisions("dedupe", "created", "unavailable")).isEqualTo(2.0);
    }

    // ---- 키 있음: Idempotency-Key ----

    @Test
    void keyedNewKeyCreatesWithKeyAndReleasesLock() {
        AtomicReference<Duration> capturedTtl = new AtomicReference<>();
        CreateRequest<Item> request = request(USER, "hello", "key-1")
                .create(k -> {
                    capturedTtl.set(guard.ttl(lockKey(USER, "key-1")));
                    Item item = new Item(ids.incrementAndGet(), "hello", k);
                    rows.put(k, item);
                    return item;
                })
                .build();

        CreateOutcome<Item> outcome = executor.execute(request);

        assertThat(capturedTtl.get()).isEqualTo(Duration.ofSeconds(30));
        assertThat(outcome.isReplayed()).isFalse();
        assertThat(outcome.getValue().key).isEqualTo("key-1");
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isFalse();
        assertThat(guard.released).containsExactly(lockKey(USER, "key-1"));
        assertThat(guard.isHeld(dedupeKey(USER, "hello"))).isFalse();
        assertThat(decisions("idempotency", "created", "available")).isEqualTo(1.0);
    }

    @Test
    void keyedExistingSameContentIsReplayedWithoutCreating() {
        rows.put("key-1", new Item(99, "hello", "key-1"));

        CreateOutcome<Item> outcome = executor.execute(request(USER, "hello", "key-1").build());

        assertThat(outcome.isReplayed()).isTrue();
        assertThat(outcome.getValue().id).isEqualTo(99);
        assertThat(ids.get()).isZero();
        assertThat(decisions("idempotency", "replayed", "available")).isEqualTo(1.0);
    }

    @Test
    void keyedExistingDifferentContentIsRejected() {
        rows.put("key-1", new Item(99, "hello", "key-1"));

        assertThatThrownBy(() -> executor.execute(request(USER, "changed", "key-1").build()))
                .isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        assertThat(ids.get()).isZero();
        assertThat(decisions("idempotency", "key_reused", "available")).isEqualTo(1.0);
    }

    @Test
    void keyedLockHeldByInFlightRequestIsRejectedAsInProgress() {
        guard.tryAcquire(lockKey(USER, "key-1"), Duration.ofSeconds(30));

        assertThatThrownBy(() -> executor.execute(request(USER, "hello", "key-1").build()))
                .isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_IN_PROGRESS);
        assertThat(ids.get()).isZero();
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isTrue();
        assertThat(guard.released).isEmpty();
        assertThat(decisions("idempotency", "in_progress", "available")).isEqualTo(1.0);
    }

    @Test
    void keyedLockHeldButAlreadyCommittedIsReplayed() {
        guard.tryAcquire(lockKey(USER, "key-1"), Duration.ofSeconds(30));
        rows.put("key-1", new Item(99, "hello", "key-1"));

        assertThat(executor.execute(request(USER, "hello", "key-1").build()).isReplayed()).isTrue();
    }

    @Test
    void keyedUniqueViolationFromConcurrentWinnerIsReplayed() {
        CreateRequest<Item> losing = request(USER, "hello", "key-1")
                .create(key -> {
                    rows.put(key, new Item(42, "hello", key)); // the winner committed first
                    throw new DataIntegrityViolationException("could not execute statement",
                            new RuntimeException("Duplicate entry '7-key-1' for key '" + CONSTRAINT + "'"));
                })
                .build();

        CreateOutcome<Item> outcome = executor.execute(losing);

        assertThat(outcome.isReplayed()).isTrue();
        assertThat(outcome.getValue().id).isEqualTo(42);
    }

    @Test
    void keyedUniqueViolationWithoutVisibleRowMeansKeyOfDeletedResource() {
        CreateRequest<Item> request = request(USER, "hello", "key-1")
                .create(key -> {
                    throw new DataIntegrityViolationException("Duplicate entry '7-key-1' for key '" + CONSTRAINT + "'");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(request))
                .isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        assertSingleDecision();
    }

    @Test
    void keyedOtherIntegrityViolationIsRethrownUnchanged() {
        DataIntegrityViolationException tooLong = new DataIntegrityViolationException("Data too long for column 'content'");
        CreateRequest<Item> request = request(USER, "hello", "key-1")
                .create(key -> {
                    throw tooLong;
                })
                .build();

        assertThatThrownBy(() -> executor.execute(request)).isSameAs(tooLong);
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isFalse();
        assertSingleDecision();
    }

    @Test
    void keyedRedisUnavailableCreatesRelyingOnDatabase() {
        guard.unavailable = true;

        CreateOutcome<Item> outcome = executor.execute(request(USER, "hello", "key-1").build());

        assertThat(outcome.isReplayed()).isFalse();
        assertThat(guard.released).isEmpty();
        assertThat(decisions("idempotency", "created", "unavailable")).isEqualTo(1.0);
    }

    @Test
    void keyedFailedCreateReleasesLock() {
        CreateRequest<Item> failing = request(USER, "hello", "key-1")
                .create(key -> {
                    throw new IllegalStateException("db down");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(failing)).isInstanceOf(IllegalStateException.class);
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isFalse();
    }

    @Test
    void recordsExactlyOneDecisionPerRequest() {
        executor.execute(request(USER, "hello", null).build());
        assertThatThrownBy(() -> executor.execute(request(USER, "hello", null).build()))
                .isInstanceOf(WriteGuardException.class);
        executor.execute(request(USER, "hello", "key-1").build());
        executor.execute(request(USER, "hello", "key-1").build());
        CreateRequest<Item> failing = request(USER, "other", null)
                .create(k -> {
                    throw new IllegalStateException("boom");
                })
                .build();
        assertThatThrownBy(() -> executor.execute(failing)).isInstanceOf(IllegalStateException.class);

        double total = meters.find(CreateGuardExecutor.METRIC).counters().stream()
                .mapToDouble(Counter::count).sum();
        assertThat(total).isEqualTo(5.0);
    }

    @Test
    void keylessFingerprintIsHeldForInFlightTtlWhileCreating() {
        AtomicReference<Duration> during = new AtomicReference<>();
        AtomicReference<Throwable> nested = new AtomicReference<>();
        CreateRequest<Item> slow = request(USER, "slow", null)
                .create(k -> {
                    during.set(guard.ttl(dedupeKey(USER, "slow")));
                    try {
                        executor.execute(request(USER, "slow", null).build());
                    } catch (RuntimeException e) {
                        nested.set(e);
                    }
                    return new Item(ids.incrementAndGet(), "slow", k);
                })
                .build();

        executor.execute(slow);

        assertThat(during.get()).isEqualTo(Duration.ofSeconds(30));
        assertThat(nested.get()).isInstanceOf(WriteGuardException.class);
        assertThat(((WriteGuardException) nested.get()).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_SUBMISSION);
        assertThat(guard.ttl(dedupeKey(USER, "slow"))).isEqualTo(Duration.ofSeconds(5));
        assertThat(ids.get()).isEqualTo(1);
    }

    @Test
    void keyedUniqueViolationWinnerWithDifferentContentIsRejected() {
        CreateRequest<Item> losing = request(USER, "hello", "key-1")
                .create(key -> {
                    rows.put(key, new Item(42, "other", key));
                    throw new DataIntegrityViolationException("Duplicate entry for key '" + CONSTRAINT + "'");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(losing))
                .isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isFalse();
        assertSingleDecision();
    }

    @Test
    void keyedLockTimeoutWithoutVisibleWinnerIsRetryableInProgress() {
        CreateRequest<Item> timedOut = request(USER, "hello", "key-1")
                .create(key -> {
                    throw new CannotAcquireLockException("Lock wait timeout exceeded");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(timedOut))
                .isInstanceOf(WriteGuardException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_IN_PROGRESS);
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isFalse();
        assertThat(decisions("idempotency", "in_progress", "available")).isEqualTo(1.0);
        assertSingleDecision();
    }

    @Test
    void keyedLockTimeoutAfterWinnerCommittedIsReplayed() {
        CreateRequest<Item> timedOut = request(USER, "hello", "key-1")
                .create(key -> {
                    rows.put(key, new Item(42, "hello", key));
                    throw new CannotAcquireLockException("Lock wait timeout exceeded");
                })
                .build();

        CreateOutcome<Item> outcome = executor.execute(timedOut);

        assertThat(outcome.isReplayed()).isTrue();
        assertThat(outcome.getValue().id).isEqualTo(42);
        assertSingleDecision();
    }

    @Test
    void keyedLookupFailureReleasesLockAndRecordsError() {
        CreateRequest<Item> request = request(USER, "hello", "key-1")
                .findByKey(k -> {
                    throw new IllegalStateException("db down");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(request))
                .isInstanceOf(IllegalStateException.class).hasMessage("db down");
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isFalse();
        assertThat(decisions("idempotency", "error", "available")).isEqualTo(1.0);
        assertThat(ids.get()).isZero();
        assertSingleDecision();
    }

    @Test
    void keyedLookupFailureAfterConflictKeepsTheConflictAsSuppressed() {
        DataIntegrityViolationException conflict =
                new DataIntegrityViolationException("Duplicate entry for key '" + CONSTRAINT + "'");
        AtomicInteger lookups = new AtomicInteger();
        CreateRequest<Item> request = request(USER, "hello", "key-1")
                .findByKey(k -> {
                    if (lookups.incrementAndGet() == 2) {
                        throw new IllegalStateException("db down");
                    }
                    return Optional.empty();
                })
                .create(k -> {
                    throw conflict;
                })
                .build();

        assertThatThrownBy(() -> executor.execute(request))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getSuppressed()).contains(conflict));
        assertSingleDecision();
    }

    @Test
    void keyedHeldWithExistingRowDoesNotReleaseTheOtherRequestsLock() {
        guard.tryAcquire(lockKey(USER, "key-1"), Duration.ofSeconds(30));
        rows.put("key-1", new Item(99, "hello", "key-1"));

        CreateOutcome<Item> outcome = executor.execute(request(USER, "hello", "key-1").build());

        assertThat(outcome.isReplayed()).isTrue();
        assertThat(outcome.getValue().id).isEqualTo(99);
        assertThat(guard.released).isEmpty();
        assertThat(guard.isHeld(lockKey(USER, "key-1"))).isTrue();
    }

    @Test
    void guardExceptionsAreTreatedAsUnavailable() {
        WriteGuard exploding = new WriteGuard() {
            @Override
            public Acquisition tryAcquire(String key, Duration ttl) {
                throw new RuntimeException("boom");
            }

            @Override
            public void release(String key, String token) {
                throw new RuntimeException("boom");
            }

            @Override
            public void retain(String key, String token, Duration ttl) {
                throw new RuntimeException("boom");
            }
        };
        CreateGuardExecutor resilient = new CreateGuardExecutor(exploding, new WriteGuardProperties(), meters);

        assertThat(resilient.execute(request(USER, "hello", null).build()).isReplayed()).isFalse();
        assertThat(resilient.execute(request(USER, "hello", "key-1").build()).isReplayed()).isFalse();

        assertThat(ids.get()).isEqualTo(2);
        assertThat(decisions("dedupe", "created", "unavailable")).isEqualTo(1.0);
        assertThat(decisions("idempotency", "created", "unavailable")).isEqualTo(1.0);
    }

    @Test
    void refusesToRunInsideATransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatThrownBy(() -> executor.execute(request(USER, "hello", null).build()))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
        assertThat(ids.get()).isZero();
    }

    @Test
    void releaseAndRetainFailuresDoNotChangeTheOutcome() {
        InMemoryWriteGuard delegate = new InMemoryWriteGuard();
        WriteGuard flaky = new WriteGuard() {
            @Override
            public Acquisition tryAcquire(String key, Duration ttl) {
                return delegate.tryAcquire(key, ttl);
            }

            @Override
            public void release(String key, String token) {
                throw new RuntimeException("boom");
            }

            @Override
            public void retain(String key, String token, Duration ttl) {
                throw new RuntimeException("boom");
            }
        };
        CreateGuardExecutor flakyExecutor = new CreateGuardExecutor(flaky, new WriteGuardProperties(), meters);

        assertThat(flakyExecutor.execute(request(USER, "hello", null).build()).isReplayed()).isFalse();

        IllegalStateException original = new IllegalStateException("s3 down");
        CreateRequest<Item> failing = request(USER, "bye", null)
                .create(k -> {
                    throw original;
                })
                .build();
        assertThatThrownBy(() -> flakyExecutor.execute(failing)).isSameAs(original);

        assertThat(flakyExecutor.execute(request(USER, "hello", "key-1").build()).isReplayed()).isFalse();
        assertThat(ids.get()).isEqualTo(2);
    }

    private void assertSingleDecision() {
        double total = meters.find(CreateGuardExecutor.METRIC).counters().stream()
                .mapToDouble(Counter::count).sum();
        assertThat(total).isEqualTo(1.0);
    }

    private CreateRequest.CreateRequestBuilder<Item> request(long userId, String content, String key) {
        return CreateRequest.<Item>builder()
                .scope("item")
                .userId(userId)
                .key(IdempotencyKey.parse(key))
                .fingerprint(Fingerprints.sha256Hex(content))
                .uniqueConstraint(CONSTRAINT)
                .findByKey(k -> Optional.ofNullable(rows.get(k)))
                .sameContent(existing -> existing.content.equals(content))
                .create(k -> {
                    Item item = new Item(ids.incrementAndGet(), content, k);
                    if (k != null) {
                        rows.put(k, item);
                    }
                    return item;
                });
    }

    private static String dedupeKey(long userId, String content) {
        return "write-guard:dedupe:item:" + userId + ":" + Fingerprints.sha256Hex(content);
    }

    private static String lockKey(long userId, String key) {
        return "write-guard:idem:item:" + userId + ":" + key;
    }

    private double decisions(String mode, String outcome, String redis) {
        return meters.get(CreateGuardExecutor.METRIC)
                .tags("scope", "item", "mode", mode, "outcome", outcome, "redis", redis)
                .counter().count();
    }

    static final class Item {
        final long id;
        final String content;
        final String key;

        Item(long id, String content, String key) {
            this.id = id;
            this.content = content;
            this.key = key;
        }
    }
}
