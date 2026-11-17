package com.longing.longing.common.writeguard;

import com.longing.longing.common.ErrorCode;
import com.longing.longing.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class WriteGuardExceptionHandlerTest {

    private final WriteGuardExceptionHandler handler = new WriteGuardExceptionHandler();

    @Test
    void inProgressIs409WithRetryAfter() {
        ResponseEntity<ApiResponse<?>> response = handler.handle(new WriteGuardException(ErrorCode.REQUEST_IN_PROGRESS));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo(409);
        assertThat(response.getBody().getError().getMessage()).isEqualTo("REQUEST_IN_PROGRESS");
    }

    @Test
    void otherErrorsUseTheirStatusWithoutRetryAfter() {
        assertThat(handler.handle(new WriteGuardException(ErrorCode.DUPLICATE_SUBMISSION)).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handle(new WriteGuardException(ErrorCode.IDEMPOTENCY_KEY_REUSED)).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        ResponseEntity<ApiResponse<?>> invalid = handler.handle(new WriteGuardException(ErrorCode.INVALID_IDEMPOTENCY_KEY));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getHeaders().containsKey(HttpHeaders.RETRY_AFTER)).isFalse();
    }
}
