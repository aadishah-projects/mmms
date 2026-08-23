package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Member;
import lombok.Getter;

@Getter
public class MemberSearchResultDto {
    private final int memberId;
    private final String firstName;
    private final String lastName;
    private final String firstNameNepali;
    private final String lastNameNepali;
    private final String title;
    private final String titleNepali;

    public MemberSearchResultDto(Member member) {
        this.memberId = member.getId();
        this.firstName = member.getFirstName();
        this.lastName = member.getLastName();
        this.firstNameNepali = member.getFirstNameNepali();
        this.lastNameNepali = member.getLastNameNepali();
        this.title = member.getTitle();
        this.titleNepali = member.getTitleNepali();
    }
}
