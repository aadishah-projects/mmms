package com.sep.mmms_backend.exceptions;

public class PasswordChangeNotAllowedException extends RuntimeException {
    public PasswordChangeNotAllowedException(ExceptionMessages message) {
        super(message.toString());
    }
}
