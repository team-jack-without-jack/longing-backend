package com.longing.longing.common;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.exceptions.AlreadyLikedException;
import com.longing.longing.common.exceptions.CustomException;
import com.longing.longing.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 존재하지 않는 요청에 대한 예외
    @ExceptionHandler(value = {NoHandlerFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ApiResponse<?> handleNoPageFoundException(Exception e) {
        log.error("GlobalExceptionHandler catch NoHandlerFoundException : {}", e.getMessage());
        return ApiResponse.fail(new CustomException(ErrorCode.NOT_FOUND_END_POINT));
    }



    // 커스텀 예외
    @ExceptionHandler(value = {CustomException.class})
    public ApiResponse<?> handleCustomException(CustomException e) {
        log.error("handleCustomException() in GlobalExceptionHandler throw CustomException : {}", e.getMessage());
        return ApiResponse.fail(e);
    }

    // 기본 예외
    @ExceptionHandler(value = {Exception.class})
    public ApiResponse<?> handleException(Exception e) {
        log.error("handleException() in GlobalExceptionHandler throw Exception : {}", e.getMessage());
        e.printStackTrace();
        return ApiResponse.fail(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(value = {ResourceNotFoundException.class})
    public ApiResponse<?> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error("handleResourceNotFoundException() in GlobalExceptionHandler: {}", e.getMessage());
        return ApiResponse.fail(new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // 이미 좋아요를 눌렀을 때 발생하는 예외 처리 (IllegalStateException)
    @ExceptionHandler(value = {AlreadyLikedException.class})
    public ApiResponse<?> handleAlreadyLikedException(AlreadyLikedException e) {
        log.error("handleAlreadyLikedException() in GlobalExceptionHandler: {}", e.getMessage());
        return ApiResponse.fail(new CustomException(ErrorCode.ALREADY_LIKED));
    }

    // 유효성 검사 실패 예외 처리
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        log.error("handleValidationException() in GlobalExceptionHandler: {}", e.getMessage());
        return ApiResponse.fail(new CustomException(ErrorCode.INVALID_INPUT));
    }

    // 멀티파트 관련 예외 처리 (RequestPart 누락 시 발생)
    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ApiResponse<?> handleMultipartException(Exception e) {
        log.error("handleMultipartException() in GlobalExceptionHandler: {}", e.getMessage());
        return ApiResponse.fail(new CustomException(ErrorCode.INVALID_INPUT));
    }

}
