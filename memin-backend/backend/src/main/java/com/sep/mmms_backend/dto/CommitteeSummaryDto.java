package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.enums.CommitteeStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CommitteeSummaryDto {
    private Integer id;
    private String name;
    private String description;
    private Integer maxNoOfMeetings;
    private CommitteeStatus status;
    private LocalDate createdDate;
    private Integer numberOfMeetings;
    private Integer numberOfMembers;

    public CommitteeSummaryDto(Committee committee) {
        this.id = committee.getId();
        this.name = committee.getName();
        if(committee.getDescription() != null)
            this.description = committee.getDescription();
        if(committee.getMaxNoOfMeetings() != null && committee.getMaxNoOfMeetings() > 0) {
            this.maxNoOfMeetings = committee.getMaxNoOfMeetings();
        }
        this.status = committee.getStatus();
        this.createdDate = committee.getCreatedDate();
        this.numberOfMeetings = committee.getMeetings().size();
        this.numberOfMembers = committee.getMemberships().size();
    }
}
