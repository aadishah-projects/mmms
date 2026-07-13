package com.sep.mmms_backend.exceptions;

public class ResourceNotAccessibleException extends RuntimeException {
    public ResourceNotAccessibleException(ExceptionMessages message) {
        super(message.toString());
    }
}
