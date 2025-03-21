package com.longing.longing.common.response;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.longing.longing.common.exceptions.CustomException;
import com.longing.longing.common.ExceptionDto;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

public class ApiResponse<T> {
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final boolean success;
    @Nullable
    private final T data;
    @Nullable
    private final ExceptionDto error;

    public ApiResponse(HttpStatus httpStatus, boolean success, @Nullable T data, @Nullable ExceptionDto error) {
        this.httpStatus = httpStatus;
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public ExceptionDto getError() {
        return error;
    }

    public static <T> ApiResponse<T> ok(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.OK, true, data, null);
    }

    public static <T> ApiResponse<T> created(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.CREATED, true, data, null);
    }

    public static <T> ApiResponse<T> fail(final CustomException e) {
        return new ApiResponse<>(e.getErrorCode().getHttpStatus(), false, null, ExceptionDto.of(e.getErrorCode()));
    }
}