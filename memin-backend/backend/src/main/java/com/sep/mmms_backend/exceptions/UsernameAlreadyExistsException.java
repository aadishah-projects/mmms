package com.sep.mmms_backend.exceptions;

public class UsernameAlreadyExistsException extends RuntimeException {
    private final String username;
    public UsernameAlreadyExistsException(String message, String username) {
        super(message);
        this.username = username;
    }

    public UsernameAlreadyExistsException(ExceptionMessages message, String username) {
        super(message.toString());
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }


}
