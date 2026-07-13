package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.enums.CommitteeStatus;
import com.sep.mmms_backend.enums.MinuteLanguage;

import java.util.List;

public class CommitteeDetailsForEditDto {
    public Integer id;
    public String name;
    public String description;
    public CommitteeStatus status;
    public Integer maxNoOfMeetings;
    public MinuteLanguage minuteLanguage;
    public MemberSearchResultDto coordinator;
    public List<MemberSearchResultWithRoleDto> membersWithRoles;

    public CommitteeDetailsForEditDto(Committee committee) {
        this.id = committee.getId();
        this.name = committee.getName();
        this.description = committee.getDescription();
        this.status = committee.getStatus();
        this.maxNoOfMeetings = committee.getMaxNoOfMeetings();
        this.minuteLanguage = committee.getMinuteLanguage();
        this.coordinator = new MemberSearchResultDto(committee.getCoordinator());

        this.membersWithRoles = committee.getSortedMemberships().stream().map(membership -> new MemberSearchResultWithRoleDto(new MemberSearchResultDto(membership.getMember()), membership.getRole())).toList();
    }

    public static class MemberSearchResultWithRoleDto {
        public MemberSearchResultDto member;
        public String role;

        MemberSearchResultWithRoleDto(MemberSearchResultDto member, String role) {
            this.member = member;
            this.role = role;
        }
    }
}
