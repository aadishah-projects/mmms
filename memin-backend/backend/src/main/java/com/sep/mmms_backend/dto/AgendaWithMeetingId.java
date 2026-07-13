package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Agenda;

public class AgendaWithMeetingId {
    public int meetingId;
    public String agenda;

    AgendaWithMeetingId(Agenda agenda) {
        this.meetingId = agenda.getMeeting().getId();
        this.agenda = agenda.getAgenda();
    }
}
