package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Meeting;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DateAndMeetingIdsDto {
    LocalDate meetingDate;
    List<MeetingSummaryDto> meetings;

    public DateAndMeetingIdsDto(LocalDate meetingDate, List<Meeting> meetings) {
        this.meetingDate = meetingDate;
        this.meetings =  meetings.stream().map(MeetingSummaryDto::new).toList();
    }
}
