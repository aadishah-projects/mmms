package com.sep.mmms_backend.service;

import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.dto.CommitteeMembershipDto;
import com.sep.mmms_backend.dto.MinuteDataDto;
import com.sep.mmms_backend.enums.MinuteLanguage;
import com.sep.mmms_backend.repository.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingMinutePreparationServiceTest {

    private final MeetingMinutePreparationService service =
            new MeetingMinutePreparationService(null, null, null, null);

    @Test
    void calculatesMeetingNumberWithoutWalkingDetachedCommitteeMeetings() {
        MeetingRepository meetingRepository = mock(MeetingRepository.class);
        Committee committee = new Committee();
        committee.setId(12);
        committee.setName("Research Committee");
        committee.setDescription("Research coordination");
        committee.setMinuteLanguage(MinuteLanguage.ENGLISH);

        Member coordinator = new Member();
        coordinator.setId(1);
        coordinator.setFirstName("Asha");
        coordinator.setLastName("Sharma");
        committee.setCoordinator(coordinator);

        Meeting firstMeeting = new Meeting();
        firstMeeting.setId(10);
        Meeting currentMeeting = new Meeting();
        currentMeeting.setId(20);
        currentMeeting.setHeldDate(LocalDate.of(2026, 8, 3));
        currentMeeting.setHeldTime(LocalTime.of(10, 30));
        currentMeeting.setHeldPlace("Board Room");

        when(meetingRepository.findByCommitteeIdWithAgendas(12))
                .thenReturn(List.of(firstMeeting, currentMeeting));

        MeetingMinutePreparationService serviceWithRepository =
                new MeetingMinutePreparationService(null, new NepaliDateService(), meetingRepository);

        MinuteDataDto result = serviceWithRepository.prepareDataForMinute(committee, currentMeeting, "writer");

        assertEquals("2", result.getMeetingNumber());
        verify(meetingRepository).findByCommitteeIdWithAgendas(12);
    }

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
    void usesMeetingTemplateSnapshotAfterCommitteeTemplateChanges() {
        Committee committee = new Committee();
        committee.setName("Research Committee");
        committee.setDescription("Research coordination");
        committee.setMinuteLanguage(MinuteLanguage.ENGLISH);
        committee.setMinuteTemplateHtml("<h1>New committee template</h1>");
        Member coordinator = new Member();
        coordinator.setId(1);
        coordinator.setFirstName("Asha");
        coordinator.setLastName("Sharma");
        committee.setCoordinator(coordinator);

        Meeting meeting = new Meeting();
        meeting.setCommittee(committee);
        meeting.setMinuteTemplateHtml("<h1>Template used for this meeting</h1>");
        meeting.setHeldDate(LocalDate.of(2026, 8, 3));
        meeting.setHeldTime(LocalTime.of(10, 30));
        meeting.setHeldPlace("Board Room");

        var result = service.prepareDataForMinute(committee, meeting, "writer");

        assertTrue(result.getMinuteContentHtml().contains("Template used for this meeting"));
        assertTrue(!result.getMinuteContentHtml().contains("New committee template"));
    }

    @Test
    void keepsStructuredSectionsDiscoverableWhenTemplateUsesParagraphLabels() {
        Member coordinator = new Member();
        coordinator.setId(1);
        coordinator.setFirstName("Asha");
        coordinator.setLastName("Sharma");

        Committee committee = new Committee();
        committee.setName("Academic Committee");
        committee.setNepaliName("\u0936\u0948\u0915\u094d\u0937\u093f\u0915 \u0938\u092e\u093f\u0924\u093f");
        committee.setDescription("Academic coordination");
        committee.setMinuteLanguage(MinuteLanguage.NEPALI);
        committee.setCoordinator(coordinator);
        committee.setMinuteTemplateHtml(
                "<p>\u0928\u093f\u0930\u094d\u0923\u092f :</p><div>@decisions</div>");

        Agenda agenda = new Agenda();
        agenda.setAgenda("Updated agenda");
        Decision decision = new Decision();
        decision.setDecision("Updated decision");

        Meeting meeting = new Meeting();
        meeting.setCommittee(committee);
        meeting.setHeldDate(LocalDate.of(2026, 8, 3));
        meeting.setHeldTime(LocalTime.of(10, 30));
        meeting.setHeldPlace("Board Room");
        meeting.setAgendas(List.of(agenda));
        meeting.setDecisions(java.util.Set.of(decision));

        var result = service.prepareDataForMinute(committee, meeting, "writer");

        assertEquals("\u0936\u0948\u0915\u094d\u0937\u093f\u0915 \u0938\u092e\u093f\u0924\u093f", result.getCommitteeName());
        assertTrue(!result.getMinuteContentHtml().contains("Updated agenda"));
        assertTrue(result.getMinuteContentHtml().contains("Updated decision"));
        assertTrue(result.getMinuteContentHtml().contains("class=\"decisions minute-structured-section\""));
        assertTrue(!result.getMinuteContentHtml().contains("<table"));
    }

    @Test
    void prefersNepaliMemberTitleWhenRenderingNepaliMinute() {
        Member member = new Member();
        member.setId(52);
        member.setTitle("Member");
        member.setTitleNepali("सदस्य");
        member.setFirstName("Asmit");
        member.setLastName("Khanal");
        member.setFirstNameNepali("अस्मित");
        member.setLastNameNepali("खनाल");

        Committee committee = new Committee();
        committee.setName("Academic Committee");
        committee.setDescription("Academic coordination");
        committee.setMinuteLanguage(MinuteLanguage.NEPALI);
        committee.setCoordinator(member);

        Meeting meeting = new Meeting();
        meeting.setCommittee(committee);
        meeting.setHeldDate(LocalDate.of(2026, 8, 3));
        meeting.setHeldTime(LocalTime.of(10, 30));
        meeting.setHeldPlace("Board Room");

        var result = service.prepareDataForMinute(committee, meeting, "writer");

        assertEquals("सदस्य अस्मित खनाल", result.getChairmanFullName());
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
