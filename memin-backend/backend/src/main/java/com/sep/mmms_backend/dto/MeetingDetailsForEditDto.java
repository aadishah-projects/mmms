package com.sep.mmms_backend.dto;


import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Getter
public class MeetingDetailsForEditDto {
    private final String committeeName;
    private final String title;
    private final LocalDate heldDate;
    private final LocalTime heldTime;
    private final String heldPlace;
    private final List<MemberSearchResultDto> selectedInvitees = new LinkedList<>();
    private final List<MemberSearchResultDto> possibleInvitees;
    private final List<DecisionDto> decisions = new ArrayList<>();
    private final List<AgendaDto> agendas = new ArrayList<>();

    public MeetingDetailsForEditDto(Meeting meeting, List<MemberSearchResultDto> possibleInvitees) {
        this.possibleInvitees = possibleInvitees;
        this.committeeName = meeting.getCommittee().getName();
        this.title = meeting.getTitle();
        this.heldDate = meeting.getHeldDate();
        this.heldTime = meeting.getHeldTime();
        this.heldPlace = meeting.getHeldPlace();
        for(Member invitee: meeting.getInvitees()) {
            this.selectedInvitees.add(new MemberSearchResultDto(invitee));
        }

        for(Decision decision: meeting.getDecisions()) {
            this.decisions.add(new DecisionDto(decision));
        }

        for(Agenda agenda: meeting.getAgendas()) {
            this.agendas.add(new AgendaDto(agenda));
        }
    }
}
