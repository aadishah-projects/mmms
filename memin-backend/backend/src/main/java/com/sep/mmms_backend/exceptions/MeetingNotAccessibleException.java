package com.sep.mmms_backend.exceptions;

public class MeetingNotAccessibleException extends RuntimeException {
    private final String meetingTitle;
    public MeetingNotAccessibleException(ExceptionMessages message, String meetingTitle) {
        super(message.toString());
        this.meetingTitle = meetingTitle;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "[Meeting Title :" + meetingTitle + "]";
    }
}
