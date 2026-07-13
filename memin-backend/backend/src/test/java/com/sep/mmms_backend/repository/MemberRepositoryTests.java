//package com.sep.mmms_backend.repository;
//
//
//import com.sep.mmms_backend.component.AuditorAwareImpl;
//import com.sep.mmms_backend.config.JpaAuditingConfiguration;
//import com.sep.mmms_backend.databuilder.AppUserBuilder;
//import com.sep.mmms_backend.databuilder.CommitteeBuilder;
//import com.sep.mmms_backend.databuilder.CommitteeMembershipBuilder;
//import com.sep.mmms_backend.databuilder.MemberBuilder;
//import com.sep.mmms_backend.entity.AppUser;
//import com.sep.mmms_backend.entity.Committee;
//import com.sep.mmms_backend.entity.CommitteeMembership;
//import com.sep.mmms_backend.entity.Member;
//import com.sep.mmms_backend.exceptions.ExceptionMessages;
//import lombok.extern.slf4j.Slf4j;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDate;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//
//@Slf4j
//@DataJpaTest(properties = {
//        "spring.jpa.properties.jakarta.persistence.validation.mode=none",
//        "spring.sql.init.mode=never" //prevent data.sql db population
//})
//
//@Import({JpaAuditingConfiguration.class, AuditorAwareImpl.class})
//@ActiveProfiles("test")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//
//public class MemberRepositoryTests {
//
//    @Autowired
//    private MemberRepository memberRepository;
//
//    @Autowired
//    private CommitteeRepository committeeRepository;
//
//    @Autowired
//    private AppUserRepository appUserRepository;
//
//    @BeforeEach
//    public void init() {
//
//        Member johnDoe;
//        Member janeDoe;
//        Member bobSmith;
//
//        // Create a test user
//        AppUser testUser = AppUserBuilder.builder().withUsername("testUser").build();
//        testUser = appUserRepository.save(testUser);
//
//        // Create a committee
//        Committee committee = CommitteeBuilder.builder().withCreatedBy(testUser).build();
//        committee = committeeRepository.save(committee);
//
//        // Create committee memberships
//        CommitteeMembership membership1 = CommitteeMembershipBuilder.builder().withCommittee(committee).build();
//        CommitteeMembership membership2 = CommitteeMembershipBuilder.builder().withCommittee(committee).build();
//        CommitteeMembership membership3 = CommitteeMembershipBuilder.builder().withCommittee(committee).build();
//
//        List<CommitteeMembership> memberships1 = new LinkedList<>();
//        memberships1.add(membership1);
//
//        List<CommitteeMembership> memberships2 = new LinkedList<>();
//        memberships2.add(membership2);
//
//        List<CommitteeMembership> memberships3 = new LinkedList<>();
//        memberships3.add(membership3);
//
//        // Create members with different names
//        johnDoe = MemberBuilder.builder()
//                .withFirstName("John")
//                .withLastName("Doe")
//                .withMemberships(memberships1)
//                .build();
//        johnDoe = memberRepository.save(johnDoe);
//
//        janeDoe = MemberBuilder.builder()
//                .withFirstName("Jane")
//                .withLastName("Doe")
//                .withMemberships(memberships2)
//                .build();
//        janeDoe = memberRepository.save(janeDoe);
//
//        bobSmith = MemberBuilder.builder()
//                .withFirstName("Bob")
//                .withLastName("Smith")
//                .withMemberships(memberships3)
//                .build();
//        bobSmith = memberRepository.save(bobSmith);
//    }
//
//    @Nested
//    @DisplayName("When searching by first name or last name")
//    @WithMockUser("testUser")
//    public class FindByFirstNameOrLastNameTests {
//
//        @Test
//        @DisplayName("should find members by first name")
//        public void findByFirstName_ShouldReturnMatchingMembers() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFirstNameOrLastName("John");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(1);
//            Assertions.assertThat(foundMembers.getFirst().getFirstName()).isEqualTo("John");
//            Assertions.assertThat(foundMembers.getFirst().getLastName()).isEqualTo("Doe");
//        }
//
//        @Test
//        @DisplayName("should find members by last name")
//        public void findByLastName_ShouldReturnMatchingMembers() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFirstNameOrLastName("Doe");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(2);
//            Assertions.assertThat(foundMembers).extracting(Member::getLastName).containsOnly("Doe");
//        }
//
//        @Test
//        @DisplayName("should find members by partial name match")
//        public void findByPartialName_ShouldReturnMatchingMembers() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFirstNameOrLastName("Jo");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(1);
//            Assertions.assertThat(foundMembers.getFirst().getFirstName()).isEqualTo("John");
//        }
//
//        @Test
//        @DisplayName("should return empty list when no match found")
//        public void findByNonExistentName_ShouldReturnEmptyList() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFirstNameOrLastName("NonExistent");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("When searching by full name")
//    @WithMockUser("testUser")
//    public class FindByFullNameTests {
//
//        @Test
//        @DisplayName("should find members when first name matches key1 OR last name matches key2")
//        public void findByFullName_FirstNameKey1LastNameKey2_ShouldReturnMatchingMembers() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFullName("John", "Doe");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(2); // Both John Doe and Jane Doe match
//            Assertions.assertThat(foundMembers).extracting(Member::getLastName).containsOnly("Doe");
//        }
//
//        @Test
//        @DisplayName("should find members when first name matches key1 but last name doesn't match key2")
//        public void findByFullName_FirstNameKey1Only_ShouldReturnMatchingMembers() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFullName("John", "NonExistent");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(1);
//            Assertions.assertThat(foundMembers.getFirst().getFirstName()).isEqualTo("John");
//        }
//
//        @Test
//        @DisplayName("should find members when last name matches key2 but first name doesn't match key1")
//        public void findByFullName_LastNameKey2Only_ShouldReturnMatchingMembers() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFullName("NonExistent", "Smith");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(1);
//            Assertions.assertThat(foundMembers.getFirst().getLastName()).isEqualTo("Smith");
//        }
//
//        @Test
//        @DisplayName("should return empty list when neither key1 nor key2 match any name")
//        public void findByFullName_NoMatches_ShouldReturnEmptyList() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFullName("NonExistent1", "NonExistent2");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).isEmpty();
//        }
//
//        @Test
//        @DisplayName("should find members with partial name matches")
//        public void findByFullName_PartialMatches_ShouldReturnMatchingMembers() {
//            // When
//            List<Member> foundMembers = memberRepository.findByFullName("Jo", "Do");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(2); // Both John Doe and Jane Doe match
//            Assertions.assertThat(foundMembers).extracting(Member::getLastName).containsOnly("Doe");
//        }
//    }
//
//    @Nested
//    @DisplayName("When finding existing members in a committee")
//    @WithMockUser("testUser")
//    public class FindExistingMembersInCommitteeTests {
//
//        private Committee committee1;
//        private Committee committee2;
//        private Member memberInCommittee1;
//        private Member memberInCommittee2;
//        private Member memberInBothCommittees;
//
//        @BeforeEach
//        public void setup() {
//            // Create a test user
//            AppUser testUser = AppUserBuilder.builder().withUsername("testUserForCommittee").build();
//            testUser = appUserRepository.save(testUser);
//
//            // Create two committees
//            committee1 = CommitteeBuilder.builder().withName("Committee 1").withCreatedBy(testUser).build();
//            committee1 = committeeRepository.save(committee1);
//
//            committee2 = CommitteeBuilder.builder().withName("Committee 2").withCreatedBy(testUser).build();
//            committee2 = committeeRepository.save(committee2);
//
//            // Create memberships for committee1
//            CommitteeMembership membership1 = CommitteeMembershipBuilder.builder().withCommittee(committee1).withRole("member").build();
//            List<CommitteeMembership> memberships1 = new LinkedList<>();
//            memberships1.add(membership1);
//
//            // Create memberships for committee2
//            CommitteeMembership membership2 = CommitteeMembershipBuilder.builder().withCommittee(committee2).withRole("member").build();
//            List<CommitteeMembership> memberships2 = new LinkedList<>();
//            memberships2.add(membership2);
//
//            // Create memberships for both committees
//            CommitteeMembership membership3a = CommitteeMembershipBuilder.builder().withCommittee(committee1).withRole("member").build();
//            CommitteeMembership membership3b = CommitteeMembershipBuilder.builder().withCommittee(committee2).withRole("member").build();
//            List<CommitteeMembership> memberships3 = new LinkedList<>();
//            memberships3.add(membership3a);
//            memberships3.add(membership3b);
//
//            // Create members with different committee memberships
//            memberInCommittee1 = MemberBuilder.builder()
//                    .withFirstName("Member")
//                    .withLastName("Committee1")
//                    .withMemberships(memberships1)
//                    .build();
//            memberInCommittee1 = memberRepository.save(memberInCommittee1);
//
//            memberInCommittee2 = MemberBuilder.builder()
//                    .withFirstName("Member")
//                    .withLastName("Committee2")
//                    .withMemberships(memberships2)
//                    .build();
//
//            memberInCommittee2 = memberRepository.save(memberInCommittee2);
//
//            memberInBothCommittees = MemberBuilder.builder()
//                    .withFirstName("Member")
//                    .withLastName("BothCommittees")
//                    .withMemberships(memberships3)
//                    .build();
//
//            //as access is also checked
//
//            memberInBothCommittees = memberRepository.save(memberInBothCommittees);
//        }
//
//        @Test
//        @DisplayName("should find members that belong to the specified committee")
//        public void findExistingMembersInCommittee_MembersInCommittee_ShouldReturnMatchingMembers() {
//            // Given
//            Set<Integer> memberIds = new HashSet<>();
//            memberIds.add(memberInCommittee1.getId());
//            memberIds.add(memberInBothCommittees.getId());
//            memberIds.add(memberInCommittee2.getId());
//
//            // When
//            List<Member> foundMembers = memberRepository.findExistingMembersInCommittee(memberIds, committee1.getId(), "testUser");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(2);
//            Assertions.assertThat(foundMembers).extracting(Member::getId)
//                    .containsExactlyInAnyOrder(memberInCommittee1.getId(), memberInBothCommittees.getId());
//            Assertions.assertThat(foundMembers).extracting(Member::getId)
//                    .doesNotContain(memberInCommittee2.getId());
//        }
//
//        @Test
//        @DisplayName("should not find members that don't belong to the specified committee")
//        public void findExistingMembersInCommittee_MembersNotInCommittee_ShouldNotReturnNonMatchingMembers() {
//            // Given
//            Set<Integer> memberIds = new HashSet<>();
//            memberIds.add(memberInCommittee2.getId());
//
//            // When
//            List<Member> foundMembers = memberRepository.findExistingMembersInCommittee(memberIds, committee1.getId(), "testUser");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).isEmpty();
//        }
//
//        @Test
//        @DisplayName("should return empty set when no member IDs are provided")
//        public void findExistingMembersInCommittee_EmptyMemberIds_ShouldReturnEmptySet() {
//            // Given
//            Set<Integer> memberIds = new HashSet<>();
//
//            // When
//            List<Member> foundMembers = memberRepository.findExistingMembersInCommittee(memberIds, committee1.getId(), "testUser");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).isEmpty();
//        }
//
//        @Test
//        @DisplayName("should find members that belong to both committees when querying for committee1")
//        public void findExistingMembersInCommittee_MemberInBothCommittees_ShouldReturnForCommittee1() {
//            // Given
//            Set<Integer> memberIds = new HashSet<>();
//            memberIds.add(memberInBothCommittees.getId());
//
//            // When
//            List<Member> foundMembers = memberRepository.findExistingMembersInCommittee(memberIds, committee1.getId(), "testUser");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(1);
//            Assertions.assertThat(foundMembers.iterator().next().getId()).isEqualTo(memberInBothCommittees.getId());
//        }
//
//        @Test
//        @DisplayName("should find members that belong to both committees when querying for committee2")
//        public void findExistingMembersInCommittee_MemberInBothCommittees_ShouldReturnForCommittee2() {
//            // Given
//            Set<Integer> memberIds = new HashSet<>();
//            memberIds.add(memberInBothCommittees.getId());
//
//            // When
//            List<Member> foundMembers = memberRepository.findExistingMembersInCommittee(memberIds, committee2.getId(), "testUser");
//
//            // Then
//            Assertions.assertThat(foundMembers).isNotNull();
//            Assertions.assertThat(foundMembers).hasSize(1);
//            Assertions.assertThat(foundMembers.iterator().next().getId()).isEqualTo(memberInBothCommittees.getId());
//        }
//    }
//
//
//    @Nested
//    @DisplayName("When finding a member by ID")
//    @WithMockUser("testUser")
//    public class FindMemberByIdTests {
//
//        private Member testMember;
//
//        @BeforeEach
//        public void setup() {
//            // Create a test user
//            AppUser testUser = AppUserBuilder.builder().withUsername("testUserForMemberById").build();
//            testUser = appUserRepository.save(testUser);
//
//            // Create a committee
//            Committee committee = CommitteeBuilder.builder().withName("Test Committee").withCreatedBy(testUser).build();
//            committee = committeeRepository.save(committee);
//
//            // Create membership for the committee
//            CommitteeMembership membership = CommitteeMembershipBuilder.builder().withCommittee(committee).withRole("member").build();
//            List<CommitteeMembership> memberships = new LinkedList<>();
//            memberships.add(membership);
//
//            // Create a test member
//            testMember = MemberBuilder.builder()
//                    .withFirstName("Test")
//                    .withLastName("Member")
//                    .withMemberships(memberships)
//                    .build();
//            testMember = memberRepository.save(testMember);
//        }
//
//        @Test
//        @DisplayName("should find a member when it exists")
//        public void findMemberById_ExistingMember_ShouldReturnMember() {
//            // When
//            Member foundMember = memberRepository.findMemberById(testMember.getId());
//
//            // Then
//            Assertions.assertThat(foundMember).isNotNull();
//            Assertions.assertThat(foundMember.getId()).isEqualTo(testMember.getId());
//            Assertions.assertThat(foundMember.getFirstName()).isEqualTo("Test");
//            Assertions.assertThat(foundMember.getLastName()).isEqualTo("Member");
//        }
//
//        @Test
//        @DisplayName("should throw MemberDoesNotExistException when member does not exist")
//        public void findMemberById_NonExistingMember_ShouldThrowException() {
//            // Given
//            int nonExistingMemberId = 9999;
//
//            // When/Then
//            Assertions.assertThatThrownBy(() -> {
//                memberRepository.findMemberById(nonExistingMemberId);
//            }).isInstanceOf(com.sep.mmms_backend.exceptions.MemberDoesNotExistException.class)
//              .hasMessageContaining(ExceptionMessages.MEMBER_DOES_NOT_EXIST.toString());
//        }
//
//    }
//
//    @Nested
//    @DisplayName("When saving a member")
//    @WithMockUser("testUser")
//    public class SaveMemberTests {
//
//        private AppUser testUser;
//        private Committee committee;
//
//        @BeforeEach
//        public void setup() {
//            // Create a test user
//            testUser = AppUserBuilder.builder().withUsername("testUserForSaveMember").build();
//            testUser = appUserRepository.save(testUser);
//
//            // Create a committee
//            committee = CommitteeBuilder.builder().withName("Test Committee").withCreatedBy(testUser).build();
//            committee = committeeRepository.save(committee);
//        }
//
//        @Test
//        @DisplayName("auditing fields should be populated")
//        public void saveMember_ShouldSetAuditingFields() {
//            // Given
//            CommitteeMembership membership = CommitteeMembershipBuilder.builder()
//                    .withCommittee(committee)
//                    .withRole("member")
//                    .build();
//            List<CommitteeMembership> memberships = new LinkedList<>();
//            memberships.add(membership);
//
//            Member member = MemberBuilder.builder()
//                    .withFirstName("Test")
//                    .withLastName("Member")
//                    .withPost("Professor")
//                    .withQualification("PhD")
//                    .withMemberships(memberships)
//                    .build();
//
//            // When
//            Member savedMember = memberRepository.save(member);
//
//
//            Assertions.assertThat(savedMember.getCreatedBy()).isNotNull();
//            Assertions.assertThat(savedMember.getModifiedBy()).isNotNull();
//            Assertions.assertThat(savedMember.getCreatedBy()).isEqualTo("testUser");
//            Assertions.assertThat(savedMember.getModifiedBy()).isEqualTo("testUser");
//
//            Assertions.assertThat(savedMember.getCreatedDate()).isNotNull();
//            Assertions.assertThat(savedMember.getModifiedDate()).isNotNull();
//            Assertions.assertThat(savedMember.getCreatedDate()).isEqualTo(LocalDate.now());
//            Assertions.assertThat(savedMember.getModifiedDate()).isEqualTo(LocalDate.now());
//        }
//
//        @Test
//        @DisplayName("should save member with all mandatory fields")
//        public void saveMember_WithMandatoryFields_ShouldSaveCorrectly() {
//            // Given
//            CommitteeMembership membership = CommitteeMembershipBuilder.builder()
//                    .withCommittee(committee)
//                    .withRole("member")
//                    .build();
//            List<CommitteeMembership> memberships = new LinkedList<>();
//            memberships.add(membership);
//
//            Member member = MemberBuilder.builder()
//                    .withFirstName("Test")
//                    .withLastName("Member")
//                    .withPost("Professor")
//                    .withQualification("PhD")
//                    .withMemberships(memberships)
//                    .build();
//
//            // When
//            Member savedMember = memberRepository.save(member);
//
//            // Then
//            Assertions.assertThat(savedMember).isNotNull();
//            Assertions.assertThat(savedMember.getId()).isGreaterThan(0);
//            Assertions.assertThat(savedMember.getFirstName()).isEqualTo("Test");
//            Assertions.assertThat(savedMember.getLastName()).isEqualTo("Member");
//            Assertions.assertThat(savedMember.getPost()).isEqualTo("Professor");
//            Assertions.assertThat(savedMember.getQualification()).isEqualTo("PhD");
//        }
//
//        @Test
//        @DisplayName("should save member with memberships")
//        public void saveMember_WithMemberships_ShouldSaveMembershipsWithMemberId() {
//            // Given
//            CommitteeMembership membership = CommitteeMembershipBuilder.builder()
//                    .withCommittee(committee)
//                    .withRole("chair")
//                    .build();
//            LinkedList<CommitteeMembership> memberships = new LinkedList<>();
//            memberships.add(membership);
//
//            Member member = MemberBuilder.builder()
//                    .withFirstName("Membership")
//                    .withLastName("Test")
//                    .withMemberships(memberships)
//                    .build();
//
//            // When
//            Member savedMember = memberRepository.save(member);
//
//            // Then
//            Assertions.assertThat(savedMember).isNotNull();
//            Assertions.assertThat(savedMember.getMemberships()).isNotNull();
//            Assertions.assertThat(savedMember.getMemberships()).hasSize(1);
//
//            CommitteeMembership savedMembership = savedMember.getMemberships().iterator().next();
//            Assertions.assertThat(savedMembership.getMember().getId()).isEqualTo(savedMember.getId());
//            Assertions.assertThat(savedMembership.getCommittee().getId()).isEqualTo(committee.getId());
//            Assertions.assertThat(savedMembership.getRole()).isEqualTo("chair");
//        }
//    }
//}
