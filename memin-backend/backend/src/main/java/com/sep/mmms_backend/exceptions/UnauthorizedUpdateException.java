package com.sep.mmms_backend.exceptions;

public class UnauthorizedUpdateException extends RuntimeException {
    public UnauthorizedUpdateException(ExceptionMessages message) {
        super(message.toString());
    }
}
