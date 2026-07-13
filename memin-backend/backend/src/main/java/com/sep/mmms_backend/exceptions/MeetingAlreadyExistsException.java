package com.sep.mmms_backend.exceptions;

import org.springframework.web.ErrorResponse;

public class MeetingAlreadyExistsException extends RuntimeException {
    public MeetingAlreadyExistsException(String message) {
        super(message);
    }

    public MeetingAlreadyExistsException(ExceptionMessages message) {
        super(message.toString());
    }

}
