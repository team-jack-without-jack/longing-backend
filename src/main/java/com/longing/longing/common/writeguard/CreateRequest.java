package com.longing.longing.common.writeguard;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
@Builder
public final class CreateRequest<T> {

    /** Redis 키 네임스페이스와 메트릭 태그: "post" | "comment". */
    @NonNull
    private final String scope;
    private final long userId;
    @NonNull
    @Builder.Default
    private final Optional<IdempotencyKey> key = Optional.empty();
    /** 키 없는 요청의 내용 지문(사용자 ID는 executor가 앞에 붙인다). */
    @NonNull
    private final String fingerprint;
    /** 키 충돌을 다른 무결성 위반과 구분하기 위한 DB unique 제약 이름. */
    @NonNull
    private final String uniqueConstraint;
    /** clientRequestId로 이 사용자의 기존 리소스 조회(자체 읽기 트랜잭션). */
    @NonNull
    private final Function<String, Optional<T>> findByKey;
    /** 기존 리소스가 이번 요청과 같은 내용인지. */
    @NonNull
    private final Predicate<T> sameContent;
    /** 생성(자체 쓰기 트랜잭션). 인자는 clientRequestId, 키 없는 요청이면 null. */
    @NonNull
    private final Function<String, T> create;
}
