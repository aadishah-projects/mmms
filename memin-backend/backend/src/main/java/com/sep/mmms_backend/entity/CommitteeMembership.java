package com.sep.mmms_backend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

/**
 * NOTE: CommitteeMembership is uniquely identified by a combination of committee_id and member_id, hence this entity will have a composite primary key.
 */

@Getter
@Setter
@Entity
@AllArgsConstructor
@Table(name = "committee_memberships")
/*This class implements Persistable because it uses 'composite primary key' which is always populated before the entity is saved, and hence JPA considers it 'Not New'

 Due to this, JPA tries to fetch the entity from the database with the poulated composite primary key value and fails.

 Hence, to make this entity we implement Persistable and implement the 'isNew()' method
*/

public class CommitteeMembership implements Persistable<CommitteeMembershipId> {
    /**
     * EmbeddedId has been used to have a composite primary key for this entity
     */
    @EmbeddedId
    private CommitteeMembershipId id = new CommitteeMembershipId();

    /**
     * when committee field of this entity is populated, the @EmbeddedId is also populated due to @MapsId used
     */

    //NOTE: here the primary key column (from the above @EmbeddedId) is also the foreign key column
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("committeeId")  //it maps a relationship field to a part of the embedded primary key
    @JoinColumn(name = "committee_id", referencedColumnName = "committee_id")
    private Committee committee;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("memberId")
    @JoinColumn(name = "member_id", referencedColumnName = "member_id")
    private Member member;


    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    /*
       BUG FIX: when two new membership is created, both will have id = 0. If i add both the membership into member or committee, only one will be added, because the memberships is tracked in a 'Set' container in both member and committee.

       The equality of membership object depends on uuid of the object. Therefore, both the newly created membership is deemed as equal by the JPA.

       First membership is accepted by the 'Set' container, but the second membership will be rejected by the 'Set' container.
     */
    private String uuid;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "display_order", nullable = false)
    private Integer order;

    public CommitteeMembership() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Transient
    private boolean isNew = true;

    @Override
    public CommitteeMembershipId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    //This method is executed after JPA determines the entity is a transient entity and before calling the entityManager.persist() method.
    public void initUUID() {
        markNotNew();
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CommitteeMembership that = (CommitteeMembership) o;
        return Objects.equals(uuid, that.uuid);
    }
}
