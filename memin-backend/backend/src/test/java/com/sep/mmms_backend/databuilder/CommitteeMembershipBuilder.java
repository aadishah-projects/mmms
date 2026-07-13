package com.sep.mmms_backend.databuilder;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.CommitteeMembershipId;
import com.sep.mmms_backend.entity.Member;

public class CommitteeMembershipBuilder {
    private Committee committee;
    private Member member;
    private String role="member";

    /**
     * if we are building membership for a member, valid committee must be referenced in this object.
     * and the member for this membership is automatically populated while associating this membership with a member.
     * and vice versa.
     */
    public static CommitteeMembershipBuilder builder() {
        return new CommitteeMembershipBuilder();
    }

    public CommitteeMembershipBuilder withCommittee(final Committee committee) {
        this.committee = committee;
        return this;
    }

    public CommitteeMembershipBuilder withMember(final Member member) {
        this.member = member;
        return this;
    }

    public CommitteeMembershipBuilder withRole(final String role) {
        this.role = role;
        return this;
    }

    public CommitteeMembership build() {
        CommitteeMembership committeeMembership = new CommitteeMembership();
        committeeMembership.setCommittee(committee);
        committeeMembership.setMember(member);
        committeeMembership.setRole(role);
        committeeMembership.setId(new CommitteeMembershipId());
        return committeeMembership;
    }
}
