package com.sep.mmms_backend.service;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.repository.AppUserRepository;
import com.sep.mmms_backend.repository.MeetingRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import com.sep.mmms_backend.validators.EntityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EntityValidator entityValidator;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private CommitteeService committeeService;

    @Mock
    private MeetingMinutePreparationService meetingMinutePreparationService;

    @InjectMocks
    private MeetingService meetingService;

    @Test
    void keepsEditedMinuteAndOnlyReordersItsAttendanceRowsWhenMeetingIsEdited() {
        Member coordinator = member(1, "Asha");
        Member firstInvitee = member(2, "Bikash");
        Member secondInvitee = member(3, "Chandra");

        Committee committee = new Committee();
        committee.setId(10);
        committee.setName("Research Committee");
        committee.setMinuteLanguage(com.sep.mmms_backend.enums.MinuteLanguage.ENGLISH);
        committee.setCoordinator(coordinator);
        committee.setMinuteTemplateHtml("<p>old frozen template</p>");

        Meeting meeting = new Meeting();
        meeting.setId(20);
        meeting.setCommittee(committee);
        meeting.setChairman(coordinator);
        meeting.setInvitees(new ArrayList<>(List.of(firstInvitee, secondInvitee)));
        meeting.setAttendees(new ArrayList<>(List.of(coordinator, firstInvitee, secondInvitee)));
        meeting.setAgendas(new ArrayList<>());
        meeting.setDecisions(new LinkedHashSet<>());
        meeting.setMinuteContentHtml(
                "<div data-edited='true'><p>edited minute text</p>"
                        + "<table class='memberships'><thead><tr><th>S.N.</th><th>Name</th>"
                        + "<th>Position</th><th>Signature</th></tr></thead><tbody>"
                        + "<tr><td>1</td><td>Asha</td><td>Coordinator</td><td>signed</td></tr>"
                        + "<tr><td>2</td><td>Bikash</td><td>Invitee</td><td></td></tr>"
                        + "<tr><td>3</td><td>Chandra</td><td>Invitee</td><td></td></tr>"
                        + "</tbody></table></div>");

        when(meetingRepository.findById(20)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(entityValidator).validate(any());
        when(committeeService.getCommitteeIfAccessible(10, "writer")).thenReturn(committee);

        var update = new com.sep.mmms_backend.dto.MeetingCreationDto();
        update.setTitle("Updated meeting");
        update.setHeldDate(LocalDate.of(2026, 8, 23));
        update.setHeldTime(LocalTime.of(10, 30));
        update.setHeldPlace("Board Room");
        update.setChairmanId(1);
        update.setInviteeIds(new LinkedHashSet<>(List.of(3, 2)));

        meetingService.updateExistingMeeting(update, 20, "writer");

        String html = meeting.getMinuteContentHtml();
        assertTrue(html.contains("data-edited=\"true\"") || html.contains("data-edited='true'"));
        assertTrue(html.indexOf("Chandra") < html.indexOf("Bikash"));
        assertTrue(!html.contains("old frozen template"));
    }

    private Member member(int id, String firstName) {
        Member member = new Member();
        member.setId(id);
        member.setFirstName(firstName);
        member.setLastName("Member");
        member.setTitle("Mr.");
        return member;
    }
}
