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
    private final int meetingNumber;
    private final MemberSearchResultDto chairman;
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
        List<Meeting> orderedMeetings = meeting.getCommittee().getMeetings().stream()
                .filter(candidate -> candidate.getId() != null)
                .sorted(java.util.Comparator.comparing(Meeting::getId))
                .toList();
        int resolvedMeetingNumber = 1;
        for (int index = 0; index < orderedMeetings.size(); index++) {
            if (java.util.Objects.equals(orderedMeetings.get(index).getId(), meeting.getId())) {
                resolvedMeetingNumber = index + 1;
                break;
            }
        }
        this.meetingNumber = resolvedMeetingNumber;
        Member meetingChairman = meeting.getChairman() != null
                ? meeting.getChairman()
                : meeting.getCommittee().getCoordinator();
        this.chairman = new MemberSearchResultDto(meetingChairman);
        Set<Integer> selectedInviteeIds = meeting.getInvitees().stream()
                .map(Member::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Integer> addedInviteeIds = new HashSet<>();
        // Attendees contain the persisted attendance order. Use it to restore
        // the selected invitee order when the meeting is opened for editing.
        for (Member attendee : meeting.getAttendees()) {
            if (selectedInviteeIds.contains(attendee.getId()) && addedInviteeIds.add(attendee.getId())) {
                this.selectedInvitees.add(new MemberSearchResultDto(attendee));
            }
        }
        for (Member invitee : meeting.getInvitees()) {
            if (addedInviteeIds.add(invitee.getId())) {
                this.selectedInvitees.add(new MemberSearchResultDto(invitee));
            }
        }

        for(Decision decision: meeting.getDecisions()) {
            this.decisions.add(new DecisionDto(decision));
        }

        for(Agenda agenda: meeting.getAgendas()) {
            this.agendas.add(new AgendaDto(agenda));
        }
    }
}
