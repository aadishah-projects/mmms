package com.sep.mmms_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommitteeMembershipDto {
    Integer memberId;
    String fullName;
    String role;

    public CommitteeMembershipDto(String fullName, String role) {
        this.fullName = fullName;
        this.role = role;
    }

    public CommitteeMembershipDto(Integer memberId, String fullName, String role) {
        this.memberId = memberId;
        this.fullName = fullName;
        this.role = role;
    }
}
