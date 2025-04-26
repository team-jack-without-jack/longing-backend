package com.longing.longing.common.exceptions;

public class AlreadyReportedPostException extends RuntimeException{
    public AlreadyReportedPostException(String message) {
        super(message);
    }
}
