package com.sep.mmms_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class CommitteeMembershipId {
    @Column(name="committee_id")
    private Integer committeeId;

    @Column(name="member_id")
    private Integer memberId;
}
