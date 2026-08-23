package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Member;
import lombok.Getter;

@Getter
public class MemberDetailsDto {
    private final int memberId;
    private final String firstName;
    private final String lastName;
    private final String firstNameNepali;
    private final String lastNameNepali;
    private final String post;
    private final String title;
    private final String titleNepali;
    private final String institution;
    private final String email;

    public MemberDetailsDto(Member member) {
        this.memberId = member.getId();
        this.firstName = member.getFirstName();
        this.lastName = member.getLastName();
        this.firstNameNepali = member.getFirstNameNepali();
        this.lastNameNepali = member.getLastNameNepali();
        this.post = member.getPost();
        this.title = member.getTitle();
        this.titleNepali = member.getTitleNepali();
        this.institution = member.getInstitution();
        this.email = member.getEmail();
    }
}
