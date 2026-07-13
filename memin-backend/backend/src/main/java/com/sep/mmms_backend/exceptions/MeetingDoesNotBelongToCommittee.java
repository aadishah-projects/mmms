package com.sep.mmms_backend.exceptions;

import lombok.Getter;

@Getter
public class MeetingDoesNotBelongToCommittee extends RuntimeException {
    private final String meetingTitle;
    private final String committeeName;
    public MeetingDoesNotBelongToCommittee(ExceptionMessages message, String meetingTitle, String committeeName) {
        super(message.toString());
        this.meetingTitle = meetingTitle;
        this.committeeName = committeeName;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "  [Meeting Title: " + meetingTitle + ", Committee Name: " + committeeName + "]";
    }
}

