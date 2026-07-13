package com.sep.mmms_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.Member;
import lombok.Getter;


/**
 * returns the summary of the member along with role of the member in a particular committee
 */
@Getter
public class MemberSummaryDto {
    private final int memberId;
    private final String firstName;
    private final String lastName;
    private final String post;
    private final String institution;
    private String role ;

    public MemberSummaryDto(Member member, int committeeId) {
        this.memberId = member.getId();
        this.firstName = member.getFirstName();
        this.lastName = member.getLastName();
        this.post = member.getPost();
        this.institution = member.getInstitution();
        for(CommitteeMembership membership: member.getMemberships() ) {
            if(membership.getId() != null
                    && membership.getId().getCommitteeId() != null
                    && membership.getId().getCommitteeId() == committeeId
            ) {
                this.role = membership.getRole();
                break;
            }
        }
//        this.firstNameNepali = member.getFirstNameNepali();
//        this.lastNameNepali = member.getLastNameNepali();
    }


    //this constructor is for testing purposes only as no-args constructor can't be made due to private fields
    //with this constructor, ObjectMapper can reconstruct a MemberSummaryDto from json
    //used in MemberControllerTests
    @JsonCreator
    public MemberSummaryDto(
            @JsonProperty("memberId") int memberId,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("institution") String institution,
            @JsonProperty("post") String post,
            @JsonProperty("role") String role
    ) {
        this.memberId = memberId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.post = post;
        this.role = role;
        this.institution = institution;
//        this.firstNameNepali = firstNameNepali;
//        this.lastNameNepali = lastNameNepali;
    }
}
