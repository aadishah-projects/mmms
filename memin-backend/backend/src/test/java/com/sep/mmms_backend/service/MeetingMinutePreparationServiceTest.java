package com.sep.mmms_backend.service;

import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.dto.CommitteeMembershipDto;
import com.sep.mmms_backend.dto.MinuteDataDto;
import com.sep.mmms_backend.enums.MinuteLanguage;
import org.junit.jupiter.api.Test;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
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
    void resolvesAtPlaceholdersInCommitteeHeader() {
        Member coordinator = new Member();
        coordinator.setId(1);
        coordinator.setTitle("Prof.");
        coordinator.setFirstName("Hari");
        coordinator.setLastName("Bahadur");

        Committee committee = new Committee();
        committee.setName("Academic Committee");
        committee.setDescription("academic policies and curriculum development");
        committee.setMinuteLanguage(MinuteLanguage.ENGLISH);
        committee.setCoordinator(coordinator);
        committee.setMinuteHeaderTemplate(
                "The @committee was held on @day, @date, at @time at @location. " +
                "Its purpose was to oversee @purpose. The meeting was coordinated by @coordinator.");

        Meeting meeting = new Meeting();
        meeting.setCommittee(committee);
        meeting.setHeldDate(LocalDate.of(2026, 8, 2));
        meeting.setHeldTime(LocalTime.of(10, 30));
        meeting.setHeldPlace("Board Room");

        var result = service.prepareDataForMinute(committee, meeting, "writer");

        assertEquals(
                "The Academic Committee was held on Sunday, 2026-08-02, at 10:30 at Board Room. " +
                "Its purpose was to oversee academic policies and curriculum development. " +
                "The meeting was coordinated by Prof. Hari Bahadur.",
                result.getHeader());
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

    @Test
    void addsAttendanceTableAndMembersToGeneratedDraftWhenMissing() {
        MinuteDataDto minuteData = new MinuteDataDto();
        minuteData.setMinuteLanguage(MinuteLanguage.ENGLISH);
        minuteData.setParticipants(List.of(
                new CommitteeMembershipDto("Dr. Asha Sharma", "Coordinator"),
                new CommitteeMembershipDto("Mr. Ram Thapa", "Member")
        ));

        String result = service.ensureAttendanceTable("<h1>Draft</h1>", minuteData);

        assertTrue(result.contains("<table>"));
        assertTrue(result.contains("Dr. Asha Sharma"));
        assertTrue(result.contains("Mr. Ram Thapa"));
        assertTrue(result.contains("Signature"));
    }

    @Test
    void convertsGenericCustomHtmlIntoNonEmptyWordDocument() throws Exception {
        String html = "<div id=\"a4-box\"><h1>Meeting Minute</h1>"
                + "<p>The meeting was held in the board room.</p>"
                + "<table><tr><th>Name</th></tr><tr><td>Asha Sharma</td></tr></table></div>";

        byte[] bytes = service.createWordDocumentFromHtml(html);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String paragraphText = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + right);
            String tableText = document.getTables().stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(row -> row.getTableCells().stream())
                    .map(cell -> cell.getText())
                    .reduce("", (left, right) -> left + right);

            assertTrue(paragraphText.contains("Meeting Minute"));
            assertTrue(paragraphText.contains("board room"));
            assertTrue(tableText.contains("Asha Sharma"));
        }
    }
}
