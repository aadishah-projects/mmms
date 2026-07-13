package com.sep.mmms_backend.databuilder;

import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;

import java.util.List;
import java.util.Set;

/*
    A member is associated with memberships, but can reference to unsaved memberships as it has CascadeType.PERSIST, however, the membership must reference valid committee and current member to populate its table

        When saving a member, it tries to save the CommitteeMembershp as well because the memberships field has CascadeType.PERSIST, however, the underlying Committee is not saved because CommitteeMembership>Committee does not have CascadeType.PERSIST. Therefore, the CascadeType.PERSIST only contributes to populating 'role' and 'id', underlying committee and member is not persisted due to memberRepository.save(member)

        Therefore, it is important that the Committeee in the Membership should be properly populated in the Membership, or else we will get an exception that foreign key for Committee is null

    A member is assocated with meetings but meetings can't be saved from Member entity as it doesn't own the relationship nor has CascadeType.PERSIST
 */

public class MemberBuilder {
    private String firstName = "firstName";
    private String lastName = "lastName";
    private String institution = "instituation";
    private String post = "post";
    private String qualification = "qualification";
    private String email = "email@gmail.com";
    private List<CommitteeMembership> memberships;
    private Set<Meeting> attendedMeetings;

    /**
        Populating 'memberships' is mandatory for save operation
     */
    public static MemberBuilder builder() {
        return new MemberBuilder();
    }

    public MemberBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public MemberBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public MemberBuilder withInstitution(String institution) {
        this.institution = institution;
        return this;
    }

    public MemberBuilder withPost(String post) {
        this.post = post;
        return this;
    }

    public MemberBuilder withQualification(String qualification) {
        this.qualification = qualification;
        return this;
    }

    public MemberBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public MemberBuilder withMemberships(List<CommitteeMembership> memberships) {
        this.memberships = memberships;
        return this;
    }

    public MemberBuilder withAttendedMeetings(Set<Meeting> attendedMeetings) {
        this.attendedMeetings = attendedMeetings;
        return this;
    }

    public Member build() {
        Member member = new Member();
        member.setFirstName(this.firstName);
        member.setLastName(this.lastName);
        member.setPost(this.post);
        if(memberships != null && !memberships.isEmpty()) {
            for(CommitteeMembership membership: memberships) {
                membership.setMember(member);
            }
        }
        member.setMemberships(this.memberships);
        return member;
    }
}
