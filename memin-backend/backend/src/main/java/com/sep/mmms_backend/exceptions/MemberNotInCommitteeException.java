package com.sep.mmms_backend.exceptions;

import lombok.Getter;

@Getter
public class MemberNotInCommitteeException extends RuntimeException {
    int memberId;
    int committeeId;
    public MemberNotInCommitteeException(ExceptionMessages message, int memberId, int committeeId) {
        super(message.toString());
        this.memberId = memberId;
        this.committeeId = committeeId;
    }
}
