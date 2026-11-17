package com.longing.longing.common.writeguard;

import com.longing.longing.common.ErrorCode;
import com.longing.longing.common.exceptions.CustomException;

/**
 * 생성 중복 방지 오류. WriteGuardExceptionHandler가 실제 HTTP 상태로 응답하고,
 * 그 처리기가 없더라도 GlobalExceptionHandler의 CustomException 처리로 오류 코드가 유지된다.
 */
public class WriteGuardException extends CustomException {

    public WriteGuardException(ErrorCode errorCode) {
        super(errorCode);
    }
}
