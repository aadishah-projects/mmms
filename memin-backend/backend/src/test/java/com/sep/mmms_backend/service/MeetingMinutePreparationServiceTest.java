package com.sep.mmms_backend.service;

import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.enums.MinuteLanguage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingMinutePreparationServiceTest {

    private final MeetingMinutePreparationService service =
            new MeetingMinutePreparationService(null, null, null, null);

    @Test
    void rendersCommitteeTemplateWithMeetingValuesAndSections() {
        Member coordinator = new Member();
        coordinator.setId(1);
        coordinator.setTitle("Dr.");
        coordinator.setFirstName("Asha");
        coordinator.setLastName("Sharma");

        Committee committee = new Committee();
        committee.setName("Research Committee");
        committee.setDescription("Research coordination");
        committee.setMinuteLanguage(MinuteLanguage.ENGLISH);
        committee.setCoordinator(coordinator);
        committee.setMinuteTemplateHtml(
                "<h1>{{committeeName}}</h1><p>{{date}} {{time}} {{place}}</p>" +
                "{{attendance}}<h2>Agendas</h2>{{agendas}}<h2>Decisions</h2>{{decisions}}");

        Agenda agenda = new Agenda();
        agenda.setAgenda("Review the annual plan");
        Decision decision = new Decision();
        decision.setDecision("Approve the annual plan");

        Meeting meeting = new Meeting();
        meeting.setCommittee(committee);
        meeting.setHeldDate(LocalDate.of(2026, 8, 3));
        meeting.setHeldTime(LocalTime.of(10, 30));
        meeting.setHeldPlace("Board Room");
        meeting.setAgendas(List.of(agenda));
        meeting.setDecisions(java.util.Set.of(decision));

        var result = service.prepareDataForMinute(committee, meeting, "writer");

        assertTrue(result.getMinuteContentHtml().contains("Research Committee"));
        assertTrue(result.getMinuteContentHtml().contains("03-08-2026") ||
                result.getMinuteContentHtml().contains("2026-08-03"));
        assertTrue(result.getMinuteContentHtml().contains("Asha Sharma"));
        assertTrue(result.getMinuteContentHtml().contains("Review the annual plan"));
        assertTrue(result.getMinuteContentHtml().contains("Approve the annual plan"));
    }

    @Test
    void prefersMeetingSpecificMinuteOverCommitteeTemplate() {
        Committee committee = new Committee();
        committee.setName("Research Committee");
        committee.setDescription("Research coordination");
        committee.setMinuteLanguage(MinuteLanguage.ENGLISH);
        Member coordinator = new Member();
        coordinator.setId(1);
        coordinator.setTitle("Ms.");
        coordinator.setFirstName("Asha");
        coordinator.setLastName("Sharma");
        committee.setCoordinator(coordinator);
        committee.setMinuteTemplateHtml("<p>committee draft</p>");

        Meeting meeting = new Meeting();
        meeting.setCommittee(committee);
        meeting.setHeldDate(LocalDate.of(2026, 8, 3));
        meeting.setHeldTime(LocalTime.of(10, 30));
        meeting.setHeldPlace("Board Room");
        meeting.setMinuteContentHtml("<p>edited meeting draft</p>");

        var result = service.prepareDataForMinute(committee, meeting, "writer");

        assertEquals("<p>edited meeting draft</p>", result.getMinuteContentHtml());
    }
}
