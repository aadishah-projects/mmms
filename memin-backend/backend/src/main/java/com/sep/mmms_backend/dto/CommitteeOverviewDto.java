package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.MinuteLanguage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CommitteeOverviewDto {
    private String name;
    private String description;
    private LocalDate createdDate;
    private int memberCount;
    private int meetingCount;
    private int decisionCount;
    private String coordinatorName;
    private LocalDate firstMeetingDate;
    private LocalDate lastMeetingDate;
    private List<DateAndMeetingIdsDto> meetings;
    private MinuteLanguage language;
}
