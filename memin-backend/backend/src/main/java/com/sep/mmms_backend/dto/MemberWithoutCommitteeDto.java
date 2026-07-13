package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class MemberWithoutCommitteeDto {
    private final int memberId;
    private final String firstName;
    private final String lastName;
    private final String post;
    private final String title;
    private final String institution;

    public MemberWithoutCommitteeDto(Member member){
        this.memberId = member.getId();
        this.firstName = member.getFirstName();
        this.lastName = member.getLastName();
        this.post = member.getPost();
        this.title = member.getTitle();
        this.institution = member.getInstitution();
    }
}
