package com.sep.mmms_backend.exceptions;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class MembershipAlreadyExistsException extends RuntimeException {
    Map<Integer, String> memberIdAndRole = new HashMap<>();
    public MembershipAlreadyExistsException(ExceptionMessages message, Map<Integer, String> memberIdAndRole) {
        super(message.toString());
        this.memberIdAndRole = memberIdAndRole;
    }
}
