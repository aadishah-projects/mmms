package com.sep.mmms_backend.exceptions;

public class UserDoesNotExistException extends RuntimeException{
    public UserDoesNotExistException(String message) {
        super(message);
    }

    public UserDoesNotExistException(ExceptionMessages message) {
        super(message.toString());
    }

    public UserDoesNotExistException() {
        super();
    }
}
