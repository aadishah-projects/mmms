package com.sep.mmms_backend.repository;

import com.sep.mmms_backend.component.AuditorAwareImpl;
import com.sep.mmms_backend.config.JpaAuditingConfiguration;
import com.sep.mmms_backend.databuilder.*;
import com.sep.mmms_backend.entity.*;
import com.sep.mmms_backend.service.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@DataJpaTest(properties = {
    "spring.jpa.properties.jakarta.persistence.validation.mode=none",
    "spring.sql.init.mode=never" //prevent data.sql db population
})
@Import({JpaAuditingConfiguration.class, AuditorAwareImpl.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MeetingRepositoryTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private CommitteeRepository committeeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @MockitoBean
    AppUserService appUserService;

    Meeting meeting;
    Member member;
    Committee committee;

    @BeforeEach
    public void init() {
        AppUser testUser = AppUserBuilder.builder().withUsername("testUser").build();
        testUser = appUserRepository.save(testUser);

        committee = CommitteeBuilder.builder().withCreatedBy(testUser).build();
        committee = committeeRepository.save(committee);

        CommitteeMembership membership = CommitteeMembershipBuilder.builder().withCommittee(committee).build();
        List<CommitteeMembership> memberships = new LinkedList<>();
        memberships.add(membership);

        member = MemberBuilder.builder().withMemberships(memberships).build();
        member = memberRepository.save(member);

    }

   @Nested
   @DisplayName("When a new meeting is created")
   @WithMockUser("testUser")
   public class CreateMeetingTests {
       @Test
       @DisplayName("auditing fields should be populated")
       public void saveMeeting_ShouldSetAuditingFields() {
           //Given:
           List<Member> attendees = new LinkedList<>();
           attendees.add(member);

           Decision decision = DecisionBuilder.builder().withDecisionText("Decision").build();
           List<Decision> decisions = new LinkedList<>();

           decisions.add(decision);
           meeting = MeetingBuilder.builder().withAttendees(attendees).withCommittee(committee).withDecisions(decisions).build();


           //When:
           Meeting savedMeeting = meetingRepository.save(meeting);

           //Then:
           Meeting foundMeeting = meetingRepository.findById(savedMeeting.getId()).orElse(null);
           Assertions.assertThat(foundMeeting).isNotNull();
           Meeting m =  foundMeeting.getDecisions().getFirst().getMeeting();

           Assertions.assertThat(foundMeeting.getCreatedBy()).isNotNull();
           Assertions.assertThat(foundMeeting.getUpdatedBy()).isNotNull();
           Assertions.assertThat(foundMeeting.getCreatedBy()).isEqualTo("testUser");
           Assertions.assertThat(foundMeeting.getUpdatedBy()).isEqualTo("testUser");

           Assertions.assertThat(foundMeeting.getCreatedDate()).isNotNull();
           Assertions.assertThat(foundMeeting.getUpdatedDate()).isNotNull();
           Assertions.assertThat(foundMeeting.getCreatedDate()).isEqualTo(LocalDate.now());
           Assertions.assertThat(foundMeeting.getUpdatedDate()).isEqualTo(LocalDate.now());
       }

       @Test
       @DisplayName("should populate attendees join table")
       public void saveMeeting_ShouldPopulateAttendees() {
           // Given
           // Create two members to be attendees
           CommitteeMembership membership1 = CommitteeMembershipBuilder.builder().withCommittee(committee).build();
           List<CommitteeMembership> memberships1 = new LinkedList<>();
           memberships1.add(membership1);

           Member attendee1 = MemberBuilder.builder()
                   .withFirstName("Attendee")
                   .withLastName("One")
                   .withMemberships(memberships1)
                   .build();
           attendee1 = memberRepository.save(attendee1);

           CommitteeMembership membership2 = CommitteeMembershipBuilder.builder().withCommittee(committee).build();
           List<CommitteeMembership> memberships2 = new LinkedList<>();
           memberships2.add(membership2);

           Member attendee2 = MemberBuilder.builder()
                   .withFirstName("Attendee")
                   .withLastName("Two")
                   .withMemberships(memberships2)
                   .build();
           attendee2 = memberRepository.save(attendee2);

           // Add both members as attendees
           List<Member> attendees = new LinkedList<>();
           attendees.add(attendee1);
           attendees.add(attendee2);

           Decision decision = DecisionBuilder.builder().withDecisionText("Test Decision").build();
           List<Decision> decisions = new LinkedList<>();
           decisions.add(decision);

           meeting = MeetingBuilder.builder()
                   .withTitle("Test Meeting")
                   .withHeldDate(LocalDate.now())
                   .withHeldTime(LocalTime.now())
                   .withHeldPlace("Test Place")
                   .withAttendees(attendees)
                   .withCommittee(committee)
                   .withDecisions(decisions)
                   .build();

           // When
           Meeting savedMeeting = meetingRepository.save(meeting);

           // Then
           Meeting foundMeeting = meetingRepository.findById(savedMeeting.getId()).orElse(null);
           Assertions.assertThat(foundMeeting).isNotNull();

           // Verify attendees are correctly associated with the meeting
           Assertions.assertThat(foundMeeting.getAttendees()).isNotNull();
           Assertions.assertThat(foundMeeting.getAttendees()).hasSize(2);
           Assertions.assertThat(foundMeeting.getAttendees()).extracting(Member::getId)
                   .containsExactlyInAnyOrder(attendee1.getId(), attendee2.getId());
       }


       @Test
       @DisplayName("should persist decisions with the meeting")
       public void saveMeeting_ShouldPersistDecisions() {
           // Given
           List<Member> attendees = new LinkedList<>();
           attendees.add(member);

           // Create multiple decisions
           Decision decision1 = DecisionBuilder.builder().withDecisionText("Decision 1").build();
           Decision decision2 = DecisionBuilder.builder().withDecisionText("Decision 2").build();
           List<Decision> decisions = new LinkedList<>();
           decisions.add(decision1);
           decisions.add(decision2);

           meeting = MeetingBuilder.builder()
                   .withTitle("Test Meeting")
                   .withHeldDate(LocalDate.now())
                   .withHeldTime(LocalTime.now())
                   .withHeldPlace("Test Place")
                   .withAttendees(attendees)
                   .withCommittee(committee)
                   .withDecisions(decisions)
                   .build();

           // When
           Meeting savedMeeting = meetingRepository.save(meeting);

           // Then
           Meeting foundMeeting = meetingRepository.findById(savedMeeting.getId()).orElse(null);
           Assertions.assertThat(foundMeeting).isNotNull();

           // Verify decisions are persisted and associated with the meeting
           Assertions.assertThat(foundMeeting.getDecisions()).isNotNull();
           Assertions.assertThat(foundMeeting.getDecisions()).hasSize(2);
           Assertions.assertThat(foundMeeting.getDecisions()).extracting(Decision::getDecision)
                   .containsExactlyInAnyOrder("Decision 1", "Decision 2");

           // Verify each decision references back to the meeting
           for (Decision decision : foundMeeting.getDecisions()) {
               Assertions.assertThat(decision.getMeeting()).isNotNull();
               Assertions.assertThat(decision.getMeeting().getId()).isEqualTo(foundMeeting.getId());
           }
       }

       @Test
       @DisplayName("should persist all meeting data correctly")
       public void saveMeeting_ShouldPersistAllData() {
           // Given
           LocalDate meetingDate = LocalDate.of(2023, 5, 15);
           LocalTime meetingTime = LocalTime.of(14, 30);
           String meetingTitle = "Important Committee Meeting";
           String meetingDescription = "Discussion about project progress";
           String meetingPlace = "Conference Room A";

           List<Member> attendees = new LinkedList<>();
           attendees.add(member);

           Decision decision = DecisionBuilder.builder().withDecisionText("Approved project timeline").build();
           List<Decision> decisions = new LinkedList<>();
           decisions.add(decision);

           meeting = MeetingBuilder.builder()
                   .withTitle(meetingTitle)
                   .withHeldDate(meetingDate)
                   .withHeldTime(meetingTime)
                   .withHeldPlace(meetingPlace)
                   .withAttendees(attendees)
                   .withCommittee(committee)
                   .withDecisions(decisions)
                   .build();

           // When
           Meeting savedMeeting = meetingRepository.save(meeting);

           // Then
           Meeting foundMeeting = meetingRepository.findById(savedMeeting.getId()).orElse(null);
           Assertions.assertThat(foundMeeting).isNotNull();

           // Verify all fields are persisted correctly
           Assertions.assertThat(foundMeeting.getTitle()).isEqualTo(meetingTitle);
           Assertions.assertThat(foundMeeting.getHeldDate()).isEqualTo(meetingDate);
           Assertions.assertThat(foundMeeting.getHeldTime()).isEqualTo(meetingTime);
           Assertions.assertThat(foundMeeting.getHeldPlace()).isEqualTo(meetingPlace);

           // Verify relationships
           Assertions.assertThat(foundMeeting.getCommittee()).isNotNull();
           Assertions.assertThat(foundMeeting.getCommittee().getId()).isEqualTo(committee.getId());

           Assertions.assertThat(foundMeeting.getAttendees()).isNotNull();
           Assertions.assertThat(foundMeeting.getAttendees()).hasSize(1);
           Assertions.assertThat(foundMeeting.getAttendees().iterator().next().getId()).isEqualTo(member.getId());

           Assertions.assertThat(foundMeeting.getDecisions()).isNotNull();
           Assertions.assertThat(foundMeeting.getDecisions()).hasSize(1);
           Assertions.assertThat(foundMeeting.getDecisions().getFirst().getDecision()).isEqualTo("Approved project timeline");
       }
   }
}
