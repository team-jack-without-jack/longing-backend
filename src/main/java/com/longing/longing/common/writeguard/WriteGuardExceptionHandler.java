package com.longing.longing.common.writeguard;

import com.longing.longing.common.ErrorCode;
import com.longing.longing.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 생성 중복 방지 오류를 실제 HTTP 상태로 응답한다(기존 GlobalExceptionHandler는 모든 오류를 HTTP 200으로 내린다).
 * GlobalExceptionHandler의 Exception 처리보다 먼저 선택되도록 최우선 순위로 둔다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class WriteGuardExceptionHandler {

    @ExceptionHandler(WriteGuardException.class)
    public ResponseEntity<ApiResponse<?>> handle(WriteGuardException exception) {
        ErrorCode error = exception.getErrorCode();
        ResponseEntity.BodyBuilder response = ResponseEntity.status(error.getHttpStatus())
                .cacheControl(CacheControl.noStore());
        if (error == ErrorCode.REQUEST_IN_PROGRESS) {
            response.header(HttpHeaders.RETRY_AFTER, "1");
        }
        return response.body(ApiResponse.fail(exception));
    }
}
