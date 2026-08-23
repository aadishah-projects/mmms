package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.enums.CommitteeStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.time.LocalDate;

@Getter
@Setter
public class CommitteeSummaryDto {
    private Integer id;
    private String name;
    private String nepaliName;
    private String description;
    private Integer maxNoOfMeetings;
    private CommitteeStatus status;
    private LocalDate createdDate;
    private Integer numberOfMeetings;
    private Integer numberOfMembers;
    private String secretaryName;

    public CommitteeSummaryDto(Committee committee) {
        this.id = committee.getId();
        this.name = committee.getName();
        this.nepaliName = committee.getNepaliName();
        if(committee.getDescription() != null)
            this.description = committee.getDescription();
        if(committee.getMaxNoOfMeetings() != null && committee.getMaxNoOfMeetings() > 0) {
            this.maxNoOfMeetings = committee.getMaxNoOfMeetings();
        }
        this.status = committee.getStatus();
        this.createdDate = committee.getCreatedDate();
        // This DTO is also created from entities returned after a service
        // transaction has ended (for example immediately after committee
        // creation). Do not dereference an uninitialized lazy collection in
        // that detached case; a newly created committee has no meetings and
        // repository summary queries fetch the collection when a real count
        // is required.
        this.numberOfMeetings = committee.getMeetings() != null
                && Hibernate.isInitialized(committee.getMeetings())
                ? committee.getMeetings().size()
                : 0;
        this.numberOfMembers = committee.getMemberships() != null
                && Hibernate.isInitialized(committee.getMemberships())
                ? committee.getSortedMemberships().size()
                : 0;
        if(committee.getSecretary() != null) {
            this.secretaryName = committee.getSecretary().getFirstName() + " " + committee.getSecretary().getLastName();
        }
    }
}
