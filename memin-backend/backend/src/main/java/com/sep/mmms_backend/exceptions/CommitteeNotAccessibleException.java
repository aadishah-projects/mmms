package com.sep.mmms_backend.exceptions;


public class CommitteeNotAccessibleException extends RuntimeException {
    private final String committeeName;

    @Deprecated
    //TODO: remove the committeeName, we don't want to leak the committee name from committee Id
    public CommitteeNotAccessibleException(ExceptionMessages message, String committeeName) {
        super(message.toString());
        this.committeeName = committeeName;
    }

    public CommitteeNotAccessibleException(ExceptionMessages message) {
        super(message.toString());
        this.committeeName = "";
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "[Committee Name: " + committeeName + "]";
    }
}
