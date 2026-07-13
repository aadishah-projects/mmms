package com.sep.mmms_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommitteeMembershipDto {
    String fullName;
    String role;
    public CommitteeMembershipDto(String fullName, String role) {
        this.fullName = fullName;
        this.role = role;
    }
}
