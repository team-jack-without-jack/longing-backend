package com.longing.longing.common.writeguard;

import com.longing.longing.common.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Optional;

/**
 * 생성 요청의 중복 여부를 판정한다. 반드시 트랜잭션 밖에서 실행해야 한다:
 * unique 위반으로 롤백된 생성 트랜잭션과 별개의 새 트랜잭션으로 기존 행을 다시 조회하기 때문이다.
 */
@Slf4j
@Component
public class CreateGuardExecutor {

    static final String METRIC = "write.guard.decisions";
    private static final String IDEMPOTENCY = "idempotency";
    private static final String DEDUPE = "dedupe";
    private static final int MAX_CAUSE_DEPTH = 16;

    private final WriteGuard guard;
    private final WriteGuardProperties properties;
    private final MeterRegistry meters;

    public CreateGuardExecutor(WriteGuard guard, WriteGuardProperties properties, MeterRegistry meters) {
        this.guard = guard;
        this.properties = properties;
        this.meters = meters;
    }

    public <T> CreateOutcome<T> execute(CreateRequest<T> request) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("CreateGuardExecutor must run outside a transaction");
        }
        Optional<IdempotencyKey> key = request.getKey();
        if (key.isPresent()) {
            return idempotent(request, key.get().value());
        }
        return deduplicated(request);
    }

    private <T> CreateOutcome<T> idempotent(CreateRequest<T> request, String key) {
        String lockKey = "write-guard:idem:" + request.getScope() + ":" + request.getUserId() + ":" + key;
        Acquisition lock = acquire(lockKey, properties.getInFlightTtl());
        Decision decision = new Decision(request.getScope(), IDEMPOTENCY, lock.isUnavailable());
        if (lock.isUnavailable()) {
            log.debug("write-guard: Redis unavailable, relying on DB unique constraint for {} (userId={})",
                    request.getScope(), request.getUserId());
        }
        try {
            Optional<T> existing = request.getFindByKey().apply(key);
            if (existing.isPresent()) {
                return replay(request, existing.get(), decision);
            }
            if (lock.isHeld()) {
                record(decision, "in_progress");
                throw new WriteGuardException(ErrorCode.REQUEST_IN_PROGRESS);
            }
            return createWithKey(request, key, decision);
        } catch (RuntimeException e) {
            recordErrorIfUndecided(decision);
            throw e;
        } finally {
            if (lock.isAcquired()) {
                release(lockKey, lock.token());
            }
        }
    }

    private <T> CreateOutcome<T> createWithKey(CreateRequest<T> request, String key, Decision decision) {
        T created;
        try {
            created = request.getCreate().apply(key);
        } catch (DataIntegrityViolationException e) {
            if (!mentions(e, request.getUniqueConstraint())) {
                throw e;
            }
            Optional<T> winner = findAfterConflict(request, key, e);
            if (winner.isPresent()) {
                return replay(request, winner.get(), decision);
            }
            // soft delete된 리소스가 이 키를 계속 점유하고 있다.
            record(decision, "key_reused");
            throw new WriteGuardException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        } catch (PessimisticLockingFailureException e) {
            // InnoDB 잠금 경합(잠금 대기 시간 초과·교착): 승자가 보이면 재생, 아니면 재시도 가능한 409.
            Optional<T> winner = findAfterConflict(request, key, e);
            if (winner.isPresent()) {
                return replay(request, winner.get(), decision);
            }
            record(decision, "in_progress");
            throw new WriteGuardException(ErrorCode.REQUEST_IN_PROGRESS);
        }
        record(decision, "created");
        return CreateOutcome.created(created);
    }

    private <T> Optional<T> findAfterConflict(CreateRequest<T> request, String key, RuntimeException conflict) {
        try {
            return request.getFindByKey().apply(key);
        } catch (RuntimeException lookupFailure) {
            lookupFailure.addSuppressed(conflict);
            throw lookupFailure;
        }
    }

    private <T> CreateOutcome<T> replay(CreateRequest<T> request, T existing, Decision decision) {
        if (!request.getSameContent().test(existing)) {
            record(decision, "key_reused");
            throw new WriteGuardException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        record(decision, "replayed");
        return CreateOutcome.replayed(existing);
    }

    private <T> CreateOutcome<T> deduplicated(CreateRequest<T> request) {
        String slotKey = "write-guard:dedupe:" + request.getScope() + ":" + request.getUserId()
                + ":" + request.getFingerprint();
        // 처리 중에는 inFlightTtl 동안 유지하고, 성공하면 완료 시점부터 dedupeWindow만 남긴다.
        Acquisition slot = acquire(slotKey, properties.getInFlightTtl());
        Decision decision = new Decision(request.getScope(), DEDUPE, slot.isUnavailable());
        boolean created = false;
        try {
            if (slot.isHeld()) {
                record(decision, "duplicate");
                throw new WriteGuardException(ErrorCode.DUPLICATE_SUBMISSION);
            }
            if (slot.isUnavailable()) {
                log.debug("write-guard: Redis unavailable, creating {} without duplicate check (userId={})",
                        request.getScope(), request.getUserId());
            }
            T value = request.getCreate().apply(null);
            created = true;
            if (slot.isAcquired()) {
                retain(slotKey, slot.token(), properties.getDedupeWindow());
            }
            record(decision, "created");
            return CreateOutcome.created(value);
        } catch (RuntimeException e) {
            recordErrorIfUndecided(decision);
            throw e;
        } finally {
            if (slot.isAcquired() && !created) {
                release(slotKey, slot.token());
            }
        }
    }

    private Acquisition acquire(String key, Duration ttl) {
        try {
            return guard.tryAcquire(key, ttl);
        } catch (RuntimeException e) {
            log.warn("write-guard: acquire failed unexpectedly, continuing without guard ({})", e.toString());
            return Acquisition.unavailable();
        }
    }

    private void release(String key, String token) {
        try {
            guard.release(key, token);
        } catch (RuntimeException e) {
            log.warn("write-guard: release failed, key expires by TTL ({})", e.toString());
        }
    }

    private void retain(String key, String token, Duration ttl) {
        try {
            guard.retain(key, token, ttl);
        } catch (RuntimeException e) {
            log.warn("write-guard: retain failed, key keeps its in-flight TTL ({})", e.toString());
        }
    }

    private static boolean mentions(Throwable error, String constraint) {
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++, cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    private void recordErrorIfUndecided(Decision decision) {
        if (!decision.recorded) {
            record(decision, "error");
        }
    }

    private void record(Decision decision, String outcome) {
        decision.recorded = true;
        meters.counter(METRIC,
                "scope", decision.scope,
                "mode", decision.mode,
                "outcome", outcome,
                "redis", decision.redisUnavailable ? "unavailable" : "available").increment();
    }

    private static final class Decision {
        private final String scope;
        private final String mode;
        private final boolean redisUnavailable;
        private boolean recorded;

        private Decision(String scope, String mode, boolean redisUnavailable) {
            this.scope = scope;
            this.mode = mode;
            this.redisUnavailable = redisUnavailable;
        }
    }
}
