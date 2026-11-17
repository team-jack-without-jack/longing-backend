package com.longing.longing.common.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.sql.SQLException;

/**
 * unique 제약으로 "없으면 삽입"을 보장한다 (#124).
 *
 * <p>동시 요청이 모두 "아직 없음"을 보고 삽입해도 DB unique 제약이 한 행만 남긴다.
 * 진 쪽의 해당 unique 위반은 "이미 있음"(false)으로 바꾸고, 다른 예외는 그대로 던진다.
 * 교착 상태·잠금 대기 시간 초과(MariaDB 1213·1205)는 한 번 다시 시도한다. 삽입이 자체 트랜잭션이라 다시 시도해도 안전하다.
 *
 * <p>호출 규칙:
 * <ul>
 *   <li>트랜잭션 밖에서만 호출한다. insert는 INSERT 하나를 자체 트랜잭션에서 실행하고 반환 전에 끝내야 한다
 *       (예: Spring Data {@code save}). 그래야 위반이 그 트랜잭션만 롤백한다. 호출자 트랜잭션 안이면
 *       위반이 그 트랜잭션을 rollback-only로 만들어 커밋 때 실패하므로 거부한다.</li>
 *   <li>open-in-view로 요청에 EntityManager가 묶여 있으면, 위반으로 롤백될 때 Spring이 그 EntityManager를
 *       clear한다. 호출 전에 읽은 엔티티는 준영속이 되므로 호출 뒤에 지연 로딩하지 않는다.</li>
 * </ul>
 */
public final class UniqueInserts {

    /** MariaDB·MySQL의 "Duplicate entry ... for key ..." 오류 코드. */
    private static final int ER_DUP_ENTRY = 1062;
    private static final String KEY_MARKER = " for key '";
    private static final int MAX_CAUSE_DEPTH = 16;

    private UniqueInserts() {
    }

    /**
     * 삽입했으면 true, 같은 행이 이미 있어 uniqueConstraint 위반이 났으면 false.
     * 교착 상태·잠금 대기 시간 초과(PessimisticLockingFailureException)는 한 번 다시 시도하고, 또 실패하면 그대로 던진다.
     *
     * @throws IllegalStateException 트랜잭션 안에서 호출한 경우(insert는 실행하지 않는다)
     */
    public static boolean insertIfAbsent(Runnable insert, String uniqueConstraint) {
        Assert.notNull(insert, "insert must not be null");
        Assert.hasText(uniqueConstraint, "uniqueConstraint must not be blank");
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "insertIfAbsent must run outside a transaction: a unique violation would mark it rollback-only");
        }
        try {
            return attempt(insert, uniqueConstraint);
        } catch (PessimisticLockingFailureException e) {
            // 같은 키를 다른 요청이 지우는 중이면 InnoDB가 이 삽입을 교착 상태로 롤백할 수 있다(1213).
            // 삽입은 자체 트랜잭션이라 이미 롤백됐으므로 한 번 다시 시도한다. 보통 "이미 있음"이나 성공으로 끝난다.
            return attempt(insert, uniqueConstraint);
        }
    }

    private static boolean attempt(Runnable insert, String uniqueConstraint) {
        try {
            insert.run();
            return true;
        } catch (DataIntegrityViolationException e) {
            if (isViolationOf(e, uniqueConstraint)) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 원인 체인의 드라이버 SQLException이 중복 키(1062)이고, 위반한 키 이름이 constraintName과 정확히 같으면 true.
     * 키 이름은 메시지의 마지막 " for key '" 뒤에서 읽는다. 그래서 앞쪽의 중복 값에 같은 문자열이 있어도 속지 않는다.
     * MySQL 8의 'table.key' 형식도 받는다.
     */
    static boolean isViolationOf(DataIntegrityViolationException error, String constraintName) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof SQLException) {
                SQLException sql = (SQLException) current;
                if (sql.getErrorCode() == ER_DUP_ENTRY && constraintName.equals(violatedKey(sql.getMessage()))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static String violatedKey(String message) {
        if (message == null) {
            return null;
        }
        int start = message.lastIndexOf(KEY_MARKER);
        if (start < 0) {
            return null;
        }
        start += KEY_MARKER.length();
        int end = message.indexOf('\'', start);
        if (end < 0) {
            return null;
        }
        String key = message.substring(start, end);
        return key.substring(key.lastIndexOf('.') + 1);
    }
}
