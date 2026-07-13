//package com.sep.mmms_backend.service;
//
//import com.sep.mmms_backend.dto.CommitteeCreationDto;
//import com.sep.mmms_backend.entity.AppUser;
//import com.sep.mmms_backend.entity.Committee;
//import com.sep.mmms_backend.entity.Member;
//import com.sep.mmms_backend.enums.CommitteeStatus;
//import com.sep.mmms_backend.exceptions.ExceptionMessages;
//import com.sep.mmms_backend.exceptions.InvalidMembershipException;
//import com.sep.mmms_backend.exceptions.MemberDoesNotExistException;
//import com.sep.mmms_backend.exceptions.ValidationFailureException;
//import com.sep.mmms_backend.repository.CommitteeMembershipRepository;
//import com.sep.mmms_backend.repository.CommitteeRepository;
//import com.sep.mmms_backend.repository.MemberRepository;
//import com.sep.mmms_backend.testing_tools.TestDataHelper;
//import com.sep.mmms_backend.validators.EntityValidator;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.validation.BindingResult;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anySet;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class CommitteeServiceTests {
//
//    //test the saveNewCommittee method
//    /*
//        1. test when the committee name is missing
//        2. test when the members in CommitteeCreationDto is missing from the databse
//        3. test when the role in one of the members in CommitteeCreationDto is null
//        4. test the success case by returning the members from the database
//     */
//
//    @Mock
//    private CommitteeRepository committeeRepository;
//
//    @Mock
//    private MemberRepository memberRepository;
//
//    @Mock
//    private CommitteeMembershipRepository committeeMembershipRepository;
//
//    @Mock
//    private EntityValidator entityValidator;
//
//    @Mock
//    private AppUserService appUserService;
//
//    @InjectMocks
//    private CommitteeService committeeService;
//
//    @Nested
//    @DisplayName("Tests for the saveNewCommittee method")
//    class SaveNewCommittee {
//        private final String username = "testUser";
//        private CommitteeCreationDto committeeCreationDto;
//        private AppUser appUser;
//        private List<Member> members;
//        private Committee savedCommittee;
//
//        @BeforeEach
//        void setUp() {
//            // Create test data
//            TestDataHelper helper = new TestDataHelper();
//            appUser = helper.getAppUser();
//            appUser.setUsername(username);
//
//            // Create valid CommitteeCreationDto
//            committeeCreationDto = new CommitteeCreationDto();
//            committeeCreationDto.setName("Test Committee");
//            committeeCreationDto.setDescription("Test Description");
//            committeeCreationDto.setStatus(CommitteeStatus.ACTIVE);
//            committeeCreationDto.setMaximumNumberOfMeetings(10);
//
//            // Create members
//            members = new ArrayList<>();
//            for (int i = 1; i <= 3; i++) {
//                Member member = new Member();
//                member.setId(i);
//                member.setFirstName("Member " + i);
//                member.setLastName("Last " + i);
//                members.add(member);
//
//                // Add member to committeeCreationDto with role
//                committeeCreationDto.getMembers().put(i, "Member");
//            }
//
//            // Create saved committee
//            savedCommittee = new Committee();
//            savedCommittee.setId(1);
//            savedCommittee.setName("Test Committee");
//            savedCommittee.setDescription("Test Description");
//            savedCommittee.setStatus(CommitteeStatus.ACTIVE);
//            savedCommittee.setMaxNoOfMeetings(10);
//            savedCommittee.setCreatedBy(appUser);
//        }
//
//        @Test
//        @DisplayName("Should throw ValidationFailureException when committee name is missing")
//        void testCommitteeNameMissing() {
//            // Arrange
//            committeeCreationDto.setName("");
//            doThrow(new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, Mockito.mock(BindingResult.class)))
//                    .when(entityValidator).validate(committeeCreationDto);
//
//            // Act & Assert
//            assertThatThrownBy(() -> committeeService.saveNewCommittee(committeeCreationDto, username))
//                    .isInstanceOf(ValidationFailureException.class)
//                    .hasMessage(ExceptionMessages.VALIDATION_FAILED.toString());
//
//            verify(entityValidator, times(1)).validate(committeeCreationDto);
//            verify(committeeRepository, never()).save(any(Committee.class));
//        }
//
//        //NOTE: the method responsible to throwing MemberDoesNotExistException with members are missing from database logic is moved to : findAndValidate() method, which should be tested separately
//
////        @Test
////        @DisplayName("Should throw MemberDoesNotExistException when members are missing from database")
////        void testMembersMissingFromDatabase() {
////            // Arrange
////            doNothing().when(entityValidator).validate(committeeCreationDto);
////
////            // Act & Assert
////            assertThatThrownBy(() -> committeeService.saveNewCommittee(committeeCreationDto, username))
////                    .isInstanceOf(MemberDoesNotExistException.class)
////                    .hasMessageContaining(ExceptionMessages.MEMBER_DOES_NOT_EXIST.toString());
////
////            verify(entityValidator, times(1)).validate(committeeCreationDto);
////            verify(memberRepository, times(1)).findAndValidateMembers(anySet());
////            verify(committeeRepository, never()).save(any(Committee.class));
////        }
//
//        @Test
//        @DisplayName("Should throw InvalidMembershipException when a member role is null")
//        void testMemberRoleNull() {
//            // Arrange
//            committeeCreationDto.getMembers().put(2, null);
//            doNothing().when(entityValidator).validate(committeeCreationDto);
//
//            // Act & Assert
//            assertThatThrownBy(() -> committeeService.saveNewCommittee(committeeCreationDto, username))
//                    .isInstanceOf(InvalidMembershipException.class)
//                    .hasMessage(ExceptionMessages.MEMBERSHIP_ROLE_MISSING.toString());
//
//            verify(entityValidator, times(1)).validate(committeeCreationDto);
//            verify(memberRepository, never()).findAllMembersById(anySet());
//            verify(committeeRepository, never()).save(any(Committee.class));
//        }
//
//        @Test
//        @DisplayName("Should successfully create committee when all validations pass")
//        void testSuccessCase() {
//            // Arrange
//            doNothing().when(entityValidator).validate(committeeCreationDto);
//            when(appUserService.loadUserByUsername(username)).thenReturn(appUser);
//            when(committeeRepository.save(any(Committee.class))).thenReturn(savedCommittee);
//
//            // Act
//            Committee result = committeeService.saveNewCommittee(committeeCreationDto, username);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(result.getId()).isEqualTo(savedCommittee.getId());
//            assertThat(result.getName()).isEqualTo(savedCommittee.getName());
//            assertThat(result.getDescription()).isEqualTo(savedCommittee.getDescription());
//            assertThat(result.getStatus()).isEqualTo(savedCommittee.getStatus());
//            assertThat(result.getMaxNoOfMeetings()).isEqualTo(savedCommittee.getMaxNoOfMeetings());
//            assertThat(result.getCreatedBy()).isEqualTo(savedCommittee.getCreatedBy());
//
//            verify(entityValidator, times(1)).validate(committeeCreationDto);
//            verify(appUserService, times(1)).loadUserByUsername(username);
//            verify(memberRepository, times(1)).findAccessibleMembersByIds(any(), any());
//            verify(memberRepository, times(1)).validateWhetherAllMembersAreFound(any(), any());
//            verify(committeeRepository, times(1)).save(any(Committee.class));
//        }
//    }
//}
