package com.sep.mmms_backend.exceptions;


public class MembershipDoesNotExistException extends RuntimeException{
    public MembershipDoesNotExistException(ExceptionMessages exceptionMessages) {
        super(exceptionMessages.toString());
    }
}
