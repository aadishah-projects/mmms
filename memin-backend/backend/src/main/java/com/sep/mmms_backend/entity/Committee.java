package com.sep.mmms_backend.entity;

import com.sep.mmms_backend.enums.CommitteeStatus;
import com.sep.mmms_backend.enums.MinuteLanguage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "committees")
@EntityListeners(AuditingEntityListener.class)
public class Committee {
    @Id
    @Column(name = "committee_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * A universally unique identifier (UUID) that serves as the business key.
     * This is the field used for equals() and hashCode().
     */
    @Column(name = "committee_uuid", nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "committee_name", nullable = false)
    private String name;

    @Column(name = "committee_description", nullable = false)
    private String description;

    @Column(name = "committee_max_no_of_meetings")
    private Integer maxNoOfMeetings;

    @Column(name = "committee_status")
    @Enumerated(EnumType.STRING)
    private CommitteeStatus status;


    @Column(name = "committee_minute_language")
    @Enumerated(EnumType.STRING)
    private MinuteLanguage minuteLanguage;

    @OneToOne
    @JoinColumn(name = "committee_coordinator_id", referencedColumnName = "member_id", nullable = false)
    private Member coordinator;

    @Column(name = "committee_created_by", updatable = false, nullable = false)
    @CreatedBy
    private String createdBy;

    @Column(name = "committee_created_date")
    @CreatedDate
    private LocalDate createdDate;

    @Column(name = "committee_modified_by")
    @LastModifiedBy
    private String modifiedBy;

    @Column(name = "committee_modified_date")
    @LastModifiedDate
    private LocalDate modifiedDate;

    @OneToMany(mappedBy = "committee", cascade = CascadeType.REMOVE)
    private List<Meeting> meetings = new ArrayList<>();

    @OneToMany(mappedBy = "committee", cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE}, orphanRemoval = true)
    private List<CommitteeMembership> memberships = new ArrayList<>();

    /**
     * automatically sets membership->committee to 'this' as well
     */
    public void addMembership(CommitteeMembership membership) {
        memberships.add(membership);
        membership.setCommittee(this);
    }

    @Transient
    ArrayList<CommitteeMembership> sortedMemberships = null;

    public List<CommitteeMembership> getSortedMemberships() {
        if (sortedMemberships == null) {
            sortedMemberships = new ArrayList<>(memberships);
            sortedMemberships.sort(Comparator.comparingInt(CommitteeMembership::getOrder));
        }
        return sortedMemberships;
    }

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Committee that = (Committee) o;
        return Objects.equals(uuid, that.uuid);
    }
}
