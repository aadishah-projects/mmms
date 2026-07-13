package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Meeting;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
public class MeetingExtendedSummaryDto {
    private final int meetingId;
    private final LocalDate meetingHeldDate;
    private final String meetingHeldPlace;
    private final LocalTime meetingHeldTime;
    private final List<String> decisions;
    private final List<String> agendas;
    private final List<String> inviteeNames;

    public MeetingExtendedSummaryDto(Meeting meeting) {
        this.meetingId = meeting.getId();
        this.meetingHeldDate = meeting.getHeldDate();
        this.meetingHeldPlace = meeting.getHeldPlace();
        this.meetingHeldTime = meeting.getHeldTime();
        this.decisions = meeting.getDecisions().stream().map(Decision::getDecision).toList();
        this.agendas = meeting.getAgendas().stream().map(Agenda::getAgenda).toList();
        this.inviteeNames = meeting.getInvitees().stream().map(invitee->invitee.getFirstName() + " " + invitee.getLastName()).toList();
    }
}
