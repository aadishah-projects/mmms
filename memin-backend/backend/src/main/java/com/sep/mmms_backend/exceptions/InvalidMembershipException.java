package com.sep.mmms_backend.exceptions;

public class InvalidMembershipException extends RuntimeException{
    public InvalidMembershipException(ExceptionMessages messages){
        super(messages.toString());
    }
}
