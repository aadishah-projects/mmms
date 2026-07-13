package com.sep.mmms_backend.exceptions;


public class MeetingDoesNotExistException extends RuntimeException{
    private int meetingId;

    public MeetingDoesNotExistException(ExceptionMessages message, int meetingId) {
        super(message.toString());
        this.meetingId = meetingId;
    }

    public MeetingDoesNotExistException() {

    }


    public int getMeetingId() {
        return meetingId;
    }
}


