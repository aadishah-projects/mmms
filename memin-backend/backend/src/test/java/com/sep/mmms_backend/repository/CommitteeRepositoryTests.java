package com.sep.mmms_backend.repository;


import com.sep.mmms_backend.component.AuditorAwareImpl;
import com.sep.mmms_backend.config.JpaAuditingConfiguration;
import com.sep.mmms_backend.databuilder.AppUserBuilder;
import com.sep.mmms_backend.databuilder.CommitteeBuilder;
import com.sep.mmms_backend.databuilder.CommitteeMembershipBuilder;
import com.sep.mmms_backend.databuilder.MemberBuilder;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.CommitteeMembership;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Slf4j
@DataJpaTest(properties = {
        "spring.jpa.properties.jakarta.persistence.validation.mode=none",
        "spring.sql.init.mode=never" //prevent data.sql db population
})

@Import({JpaAuditingConfiguration.class, AuditorAwareImpl.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

public class CommitteeRepositoryTests {

    @Autowired
    private CommitteeRepository committeeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @MockitoBean
    AppUserService appUserService;

    AppUser testUser;

    @BeforeEach
    public void init() {
        testUser = AppUserBuilder.builder().withUsername("testUser").build();
        testUser = appUserRepository.save(testUser);
    }


    @Nested
    @DisplayName("When a new committee is created")
    @WithMockUser("testUser")
    public class CreateCommitteeTests {

        Committee committee;

        @Test
        @DisplayName("auditing fields should be populated")
        public void saveCommittee_ShouldSetAuditingFields() {
            // Given
            committee = CommitteeBuilder.builder()
                    .withName("Test Committee")
                    .withDescription("Test Description")
                    .withCreatedBy(testUser)
                    .build();

            // When
            Committee savedCommittee = committeeRepository.save(committee);

            // Then
            Committee foundCommittee = committeeRepository.findById(savedCommittee.getId()).orElse(null);
            Assertions.assertThat(foundCommittee).isNotNull();

            Assertions.assertThat(foundCommittee.getCreatedBy()).isNotNull();
            Assertions.assertThat(foundCommittee.getModifiedBy()).isNotNull();
            Assertions.assertThat(foundCommittee.getModifiedBy()).isEqualTo("testUser");

            Assertions.assertThat(foundCommittee.getCreatedDate()).isNotNull();
            Assertions.assertThat(foundCommittee.getModifiedDate()).isNotNull();
            Assertions.assertThat(foundCommittee.getCreatedDate()).isEqualTo(LocalDate.now());
            Assertions.assertThat(foundCommittee.getModifiedDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("fields should be properly saved")
        public void saveCommittee_ShouldSaveFields() {
            // Given
            String committeeName = "Academic Committee";
            String committeeDescription = "Committee for academic affairs";

            committee = CommitteeBuilder.builder()
                    .withName(committeeName)
                    .withDescription(committeeDescription)
                    .withCreatedBy(testUser)
                    .build();

            // When
            Committee savedCommittee = committeeRepository.save(committee);

            // Then
            Committee foundCommittee = committeeRepository.findById(savedCommittee.getId()).orElse(null);
            Assertions.assertThat(foundCommittee).isNotNull();

            Assertions.assertThat(foundCommittee.getName()).isEqualTo(committeeName);
            Assertions.assertThat(foundCommittee.getDescription()).isEqualTo(committeeDescription);
            Assertions.assertThat(foundCommittee.getCreatedBy()).isEqualTo(testUser.getUsername());
        }

        @Test
        @DisplayName("committee membership should be saved with committee")
        public void saveCommittee_ShouldSaveMembership() {
            // Given
            // Create a committee

            committee = CommitteeBuilder.builder()
                    .withName("Test Committee")
                    .withDescription("Test Description")
                    .withCreatedBy(testUser)
                    .build();

            // Create a membership with the committee
            CommitteeMembership membership = CommitteeMembershipBuilder.builder()
                    .withCommittee(committee)
                    .withRole("Chair")
                    .build();

            List<CommitteeMembership> memberships = new LinkedList<>();
            memberships.add(membership);

            // Create a member with the membership
            Member member = MemberBuilder.builder()
                    .withFirstName("John")
                    .withLastName("Doe")
                    .build();

            // Save the member first to get an ID
            member = memberRepository.save(member);

            //Associated the membership with the 'saved' member
            membership.setMember(member);

            // Associate the membership with the committee
            committee.setMemberships(memberships);

            // When
            Committee savedCommittee = committeeRepository.save(committee);

            // Then
            Committee foundCommittee = committeeRepository.findById(savedCommittee.getId()).orElse(null);
            Assertions.assertThat(foundCommittee).isNotNull();

            // Verify memberships are saved and associated with the committee
            Assertions.assertThat(foundCommittee.getMemberships()).isNotNull();
            Assertions.assertThat(foundCommittee.getMemberships()).hasSize(1);

            CommitteeMembership foundMembership = foundCommittee.getMemberships().iterator().next();
            Assertions.assertThat(foundMembership.getRole()).isEqualTo("Chair");
            Assertions.assertThat(foundMembership.getMember().getId()).isEqualTo(member.getId());
            Assertions.assertThat(foundMembership.getMember().getFirstName()).isEqualTo("John");
            Assertions.assertThat(foundMembership.getMember().getLastName()).isEqualTo("Doe");
            Assertions.assertThat(foundMembership.getCommittee().getId()).isEqualTo(foundCommittee.getId());
        }
    }

    @Nested
    @DisplayName("When finding a committee by ID")
    @WithMockUser("testUser")
    public class FindCommitteeByIdTests {

        Committee committee;
        @BeforeEach
        public void setup() {
            // Create a test user
            AppUser testUser = AppUserBuilder.builder().withUsername("testUserForMemberById").build();
            testUser = appUserRepository.save(testUser);

            // Create a committee
            committee = CommitteeBuilder.builder().withName("Test Committee").withDescription("Test Description").withCreatedBy(testUser).build();
            committee = committeeRepository.save(committee);

        }

        @Test
        @DisplayName("should find a committee when it exists")
        public void findCommitteeById_ExistingCommittee_ShouldReturnCommittee() {
            // When
            committeeRepository.save(committee);

            Committee foundCommittee = committeeRepository.findCommitteeById(committee.getId());

            // Then
            Assertions.assertThat(foundCommittee).isNotNull();
            Assertions.assertThat(foundCommittee.getId()).isEqualTo(committee.getId());
            Assertions.assertThat(foundCommittee.getName()).isEqualTo("Test Committee");
            Assertions.assertThat(foundCommittee.getDescription()).isEqualTo("Test Description");
        }

        @Test
        @DisplayName("should throw CommitteeDoesNotExistException when committee does not exist")
        public void findCommitteeById_NonExistingCommittee_ShouldThrowException() {
            // Given
            int nonExistingCommitteeId = 9999;

            // When/Then
            Assertions.assertThatThrownBy(() -> {
                        committeeRepository.findCommitteeById(nonExistingCommitteeId);
                    }).isInstanceOf(com.sep.mmms_backend.exceptions.CommitteeDoesNotExistException.class)
                    .hasMessageContaining(ExceptionMessages.COMMITTEE_DOES_NOT_EXIST.toString());
        }
    }
}
