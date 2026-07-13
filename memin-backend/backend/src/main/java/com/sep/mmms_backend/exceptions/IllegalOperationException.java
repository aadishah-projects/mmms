package com.sep.mmms_backend.exceptions;

public class IllegalOperationException extends RuntimeException{
    public IllegalOperationException(ExceptionMessages message){
        super(message.toString());
    }

    public IllegalOperationException(String message){
        super(message);
    }

    public IllegalOperationException() {

    };
}
