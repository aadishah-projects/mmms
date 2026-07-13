package com.sep.mmms_backend.exceptions;


public enum ExceptionMessages {
    USERNAME_ALREADY_EXISTS("User with the username already exists"),
    MEETING_ALREADY_EXISTS("Meeting with the meeting already exists"),
    MEETING_DOES_NOT_EXIST("The specified meeting does not exist"),
    MEETING_NOT_IN_COMMITTEE("Meeting does not belong to the committee specified"),
    USER_DOES_NOT_EXIST("User does not exist"),
    USER_CAN_ONLY_UPDATE_THEIR_OWN_DATA("A user can only update their own data"),
    ROUTE_UPDATE_USER_CANT_UPDATE_PASSWORD("This route can't be used to update password"),
    USER_VALIDATION_FALIED("User data does not meet validation criterias"),
    MEMBER_DOES_NOT_EXIST("The specified member does not exist"),
    MEMBER_DOES_NOT_EXIST_OR_NOT_PART_OF_COMMITTEE("The specified member does not exist or not part of the committee"),
    INVALID_MEMBERSHIP("Invalid membership"),
    VALIDATION_FAILED("Validation Failed"),
    COMMITTEE_DOES_NOT_EXIST("The specified committee does not exist"),
    INVALID_MEMBERSHIP_FOR_NEW_MEMBER("The new member must have single membership"),
    COMMITTEE_NOT_ACCESSIBLE("Specified committee is not accessible to you"),
    MEMBER_NOT_IN_COMMITTEE("The specified member is not associated with the committee"),
    MEMBER_NOT_ACCESSIBLE("Specified member is not accessible to you"),
    MEMBERSHIP_ROLE_MISSING("Membership role missing"),

    MEETING_NOT_ACCESSIBLE("Meeting not accessible"),
    COMMITTEE_OR_MEETING_NOT_ACCESSIBLE("Committee or meeting not accessible"),
    MEMBERSHIP_ALREADY_EXISTS("Membership already exists"),
    MEETING_DOES_NOT_BELONG_TO_COMMITTEE("The specified meeting does not belong to the committee"),
    MEMBERSHIP_DOES_NOT_EXIST("Requested membership does not exist"),

    COORDINATOR_DOES_NOT_EXIST("The specified coordinator profile does not exist"),
    TODO("Implement Exception Handling Properly");
    ;


    final private String message;
    ExceptionMessages(String message) {
        this.message = message;
    }

    public String toString() {
        return message;
    }
}
