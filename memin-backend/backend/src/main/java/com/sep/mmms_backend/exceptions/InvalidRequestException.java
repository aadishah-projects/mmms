package com.sep.mmms_backend.exceptions;


public class InvalidRequestException extends RuntimeException{
    public InvalidRequestException(ExceptionMessages ex){
        super(ex.toString());
    }
}
