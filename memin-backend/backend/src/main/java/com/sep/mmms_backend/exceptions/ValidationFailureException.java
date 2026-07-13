package com.sep.mmms_backend.exceptions;


import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;

@Getter
public class ValidationFailureException extends RuntimeException {
    final private String message;
    final private Errors errors;

    public ValidationFailureException(ExceptionMessages message, Errors errors) {
        this.message = message.toString();
        this.errors = errors;
    }
}
