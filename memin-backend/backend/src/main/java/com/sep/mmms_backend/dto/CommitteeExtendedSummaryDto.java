package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.enums.MinuteLanguage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommitteeExtendedSummaryDto {
    private final int committeeId;
    private final String name;
    private final String description;
    private final MinuteLanguage language;
    List<MeetingExtendedSummaryDto> meetings = new ArrayList<>();

    public CommitteeExtendedSummaryDto(Committee committee) {
        this.committeeId = committee.getId();
        this.name = committee.getName();
        this.description = committee.getDescription();
        this.language = committee.getMinuteLanguage();
        committee.getMeetings().forEach(meeting -> meetings.add(new MeetingExtendedSummaryDto(meeting)));
    }
}
