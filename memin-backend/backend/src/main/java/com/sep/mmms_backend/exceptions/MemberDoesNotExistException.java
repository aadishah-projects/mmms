package com.sep.mmms_backend.exceptions;

import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
public class MemberDoesNotExistException extends RuntimeException {
    private final List<Integer> memberIds = new LinkedList<>();
    public MemberDoesNotExistException(ExceptionMessages message, int memberId){
        super(message.toString());
        this.memberIds.add(memberId);
    }

    public MemberDoesNotExistException(ExceptionMessages message, MemberDoesNotExistException cause){
        super(message.toString());
        this.memberIds.addAll(cause.getMemberIds());
    }


    public MemberDoesNotExistException(ExceptionMessages message, List<Integer> memberIds){
        super(message.toString());
        if(memberIds != null)
            this.memberIds.addAll(memberIds);
    }
}
