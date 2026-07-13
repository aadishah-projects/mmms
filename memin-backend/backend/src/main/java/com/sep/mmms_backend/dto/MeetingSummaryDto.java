package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Meeting;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class MeetingSummaryDto {
    private final int id;
    private final String title;
    private final LocalDate heldDate;
    private final LocalTime heldTime;
    private final String heldPlace;
    private final LocalDate createdDate;

    public MeetingSummaryDto(Meeting meeting) {
        this.id = meeting.getId();
        this.title = meeting.getTitle();
        this.heldDate = meeting.getHeldDate();
        this.heldTime = meeting.getHeldTime();
        this.heldPlace = meeting.getHeldPlace();
        this.createdDate = meeting.getCreatedDate();
    }
}
