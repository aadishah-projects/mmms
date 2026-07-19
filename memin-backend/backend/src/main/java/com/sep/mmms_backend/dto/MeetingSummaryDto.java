package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Meeting;
import lombok.Getter;
import org.hibernate.Hibernate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Getter
public class MeetingSummaryDto {
    private final int id;
    private final String title;
    private final LocalDate heldDate;
    private final LocalTime heldTime;
    private final String heldPlace;
    private final LocalDate createdDate;
    private final List<String> agendas;

    public MeetingSummaryDto(Meeting meeting) {
        this.id = meeting.getId();
        this.title = meeting.getTitle();
        this.heldDate = meeting.getHeldDate();
        this.heldTime = meeting.getHeldTime();
        this.heldPlace = meeting.getHeldPlace();
        this.createdDate = meeting.getCreatedDate();
        // Surface each meeting's agenda items so they are visible on the meeting cards.
        // Only read them when the collection was actually fetched — some callers (e.g. the
        // committee overview/calendar) build this DTO from detached meetings whose agendas
        // are left lazy, and touching them there would throw LazyInitializationException.
        this.agendas = (meeting.getAgendas() != null && Hibernate.isInitialized(meeting.getAgendas()))
                ? meeting.getAgendas().stream().map(Agenda::getAgenda).toList()
                : Collections.emptyList();
    }
}
