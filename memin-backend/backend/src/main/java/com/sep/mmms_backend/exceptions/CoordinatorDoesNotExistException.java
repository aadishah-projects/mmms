package com.sep.mmms_backend.exceptions;

public class CoordinatorDoesNotExistException extends RuntimeException{
    public CoordinatorDoesNotExistException(ExceptionMessages message) {
        super(message.toString());
    }
}
