package com.longing.longing.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Test Error
    TEST_ERROR(10000, HttpStatus.BAD_REQUEST, "테스트 에러입니다."),
    // 404 Not Found
    NOT_FOUND_END_POINT(400, HttpStatus.NOT_FOUND, "존재하지 않는 API입니다."),
    INVALID_INPUT(400, HttpStatus.NOT_FOUND, "INVALID_INPUT"),
    RESOURCE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),



    EMPTY_FILE_EXCEPTION(500, HttpStatus.INTERNAL_SERVER_ERROR, "EMPTY_FILE_EXCEPTION"),
    IO_EXCEPTION_ON_IMAGE_UPLOAD(500, HttpStatus.INTERNAL_SERVER_ERROR, "IO_EXCEPTION_ON_IMAGE_UPLOAD"),
    INVALID_FILE_EXTENTION(500, HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_FILE_EXTENTION"),
    NO_FILE_EXTENTION(500, HttpStatus.INTERNAL_SERVER_ERROR, "NO_FILE_EXTENTION"),
    PUT_OBJECT_EXCEPTION(500, HttpStatus.INTERNAL_SERVER_ERROR, "PUT_OBJECT_EXCEPTION"),
    IO_EXCEPTION_ON_IMAGE_DELETE(500, HttpStatus.INTERNAL_SERVER_ERROR, "IO_EXCEPTION_ON_IMAGE_DELETE"),
    ALREADY_LIKED(400, HttpStatus.BAD_REQUEST, "ALREADY_LIKED"),
    ALREADY_REPORTED(400, HttpStatus.BAD_REQUEST, "ALREADY_REPORTED"),

    // 게시글·댓글 생성 중복 방지 (#122). message 값은 클라이언트가 분기에 쓰는 API 계약이므로 사용자 문구로 바꾸지 않는다.
    INVALID_IDEMPOTENCY_KEY(400, HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY"),
    REQUEST_IN_PROGRESS(409, HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS"),
    DUPLICATE_SUBMISSION(409, HttpStatus.CONFLICT, "DUPLICATE_SUBMISSION"),
    IDEMPOTENCY_KEY_REUSED(422, HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED");


    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;
}
