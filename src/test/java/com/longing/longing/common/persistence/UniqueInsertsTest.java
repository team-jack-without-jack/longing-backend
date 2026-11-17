package com.longing.longing.common.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.PersistenceException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniqueInsertsTest {

    private static final String UK = "uk_post_bookmarks_user_post";

    @AfterEach
    void resetTransactionFlag() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void 해당_unique_제약의_중복이면_해당_위반이다() {
        assertThat(UniqueInserts.isViolationOf(duplicateOn("'" + UK + "'"), UK)).isTrue();
    }

    @Test
    void MySQL_8의_테이블_접두사_형식도_인식한다() {
        assertThat(UniqueInserts.isViolationOf(duplicateOn("'post_bookmarks." + UK + "'"), UK)).isTrue();
    }

    @Test
    void 다른_unique_제약이면_해당_위반이_아니다() {
        assertThat(UniqueInserts.isViolationOf(duplicateOn("'uk_user_blocks_user_blocked'"), UK)).isFalse();
    }

    @Test
    void 이름이_더_긴_제약이면_해당_위반이_아니다() {
        assertThat(UniqueInserts.isViolationOf(duplicateOn("'" + UK + "_v2'"), UK)).isFalse();
    }

    @Test
    void 중복_값에_제약_이름이_들어_있어도_속지_않는다() {
        SQLException sql = new SQLIntegrityConstraintViolationException(
                "Duplicate entry 'a for key '" + UK + "'' for key 'uk_users_nickname'", "23000", 1062);

        assertThat(UniqueInserts.isViolationOf(translated(sql), UK)).isFalse();
    }

    @Test
    void 외래_키_위반은_해당_위반이_아니다() {
        assertThat(UniqueInserts.isViolationOf(foreignKeyViolation(), UK)).isFalse();
    }

    @Test
    void 원인이_순환해도_끝난다() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        assertThat(UniqueInserts.isViolationOf(new DataIntegrityViolationException("top", a), UK)).isFalse();
    }

    @Test
    void 삽입에_성공하면_true() {
        AtomicInteger calls = new AtomicInteger();

        assertThat(UniqueInserts.insertIfAbsent(calls::incrementAndGet, UK)).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void 해당_unique_위반이면_false() {
        assertThat(UniqueInserts.insertIfAbsent(() -> {
            throw duplicateOn("'" + UK + "'");
        }, UK)).isFalse();
    }

    @Test
    void 다른_무결성_위반은_그대로_던진다() {
        DataIntegrityViolationException fk = foreignKeyViolation();

        assertThatThrownBy(() -> UniqueInserts.insertIfAbsent(() -> {
            throw fk;
        }, UK)).isSameAs(fk);
    }

    @Test
    void 트랜잭션_안에서는_삽입하지_않고_거부한다() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> UniqueInserts.insertIfAbsent(calls::incrementAndGet, UK))
                .isInstanceOf(IllegalStateException.class);
        assertThat(calls).hasValue(0);
    }

    @Test
    void 제약_이름이_비어_있으면_삽입하지_않고_거부한다() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> UniqueInserts.insertIfAbsent(calls::incrementAndGet, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).hasValue(0);
    }

    @Test
    void 교착_상태로_실패하면_한_번_다시_시도한다() {
        AtomicInteger calls = new AtomicInteger();

        boolean inserted = UniqueInserts.insertIfAbsent(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("Deadlock found when trying to get lock");
            }
        }, UK);

        assertThat(inserted).isTrue();
        assertThat(calls).hasValue(2);
    }

    @Test
    void 다시_시도해서_중복이면_false() {
        AtomicInteger calls = new AtomicInteger();

        boolean inserted = UniqueInserts.insertIfAbsent(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("Deadlock found when trying to get lock");
            }
            throw duplicateOn("'" + UK + "'");
        }, UK);

        assertThat(inserted).isFalse();
        assertThat(calls).hasValue(2);
    }

    @Test
    void 다시_시도해도_잠금_실패면_그대로_던진다() {
        AtomicInteger calls = new AtomicInteger();
        CannotAcquireLockException second = new CannotAcquireLockException("again");

        assertThatThrownBy(() -> UniqueInserts.insertIfAbsent(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("first");
            }
            throw second;
        }, UK)).isSameAs(second);
        assertThat(calls).hasValue(2);
    }

    /** MariaDB 드라이버가 내는 중복 키 오류(1062)를 운영과 같은 경로로 변환한다. quotedKey 예: 'uk_x' */
    private static DataIntegrityViolationException duplicateOn(String quotedKey) {
        return translated(new SQLIntegrityConstraintViolationException(
                "(conn=7) Duplicate entry '1-1001' for key " + quotedKey, "23000", 1062));
    }

    private static DataIntegrityViolationException foreignKeyViolation() {
        return translated(new SQLIntegrityConstraintViolationException(
                "(conn=7) Cannot add or update a child row: a foreign key constraint fails (`test`.`post_bookmarks`, "
                        + "CONSTRAINT `FKabc` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`))", "23000", 1452));
    }

    /** 운영과 같은 모양: 드라이버 SQLException → Hibernate ConstraintViolationException → JPA PersistenceException → Spring 변환 */
    private static DataIntegrityViolationException translated(SQLException cause) {
        return (DataIntegrityViolationException) new HibernateJpaDialect().translateExceptionIfPossible(
                new PersistenceException(new ConstraintViolationException("could not execute statement", cause, null)));
    }
}
