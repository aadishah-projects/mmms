package com.sep.mmms_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberOfCommitteeDto {
    int id;
    String name;
    String role;
    public MemberOfCommitteeDto(int id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }
}
