package com.sep.mmms_backend.service;


import com.sep.mmms_backend.dto.MemberCreationDto;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
import com.sep.mmms_backend.exceptions.ValidationFailureException;
import com.sep.mmms_backend.repository.CommitteeRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import com.sep.mmms_backend.testing_tools.TestDataHelper;
import com.sep.mmms_backend.validators.EntityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

//@SpringBootTest(classes={LocalValidatorFactoryBean.class, MemberService.class, AppConfig.class})

/*
    With @ExtendWith(MockitoExtension.class) tests a single Java class in complete isolation, without invlvoing the Spring Framework at all.

    What it does: It does not create any ApplicationContext. It's just JUnit and Mockito working together.

    Mechanism: @InjectMocks uses reflection to create an instance of our service classes(which is injected in the test class) and injects the fields of the service with the @Mock(not @MockBean) that we have made(not from the ApplicationContext)

    It is perfect for testing the business logic within a single classs(MemberSerivce) without caring about the Spring framework
 */
@ExtendWith(MockitoExtension.class)
public class MemberServiceTests {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommitteeRepository committeeRepository;

    @Mock
    private EntityValidator entityValidator;

    @InjectMocks
    private MemberService memberService;

    /*
        test the saveNewMember of MemberService

        The saveNewMember accepts a MemberCreationDto, committeeId, and username supplied from the controller.

        1. test when firstName, lastName is blank
        2. test when post is blank
        3. test when email format is not correct
        4. test when role is blank.
        5. test when firstNameNepali, lastNameNepali, institution is blank/null (nothing happens as it can be blank)
        6. test the success case with firstNameNepali, lastNameNepali

     */
    @Nested
    @DisplayName("Tests for the saveNewMember method")
    class SaveNewMember {
        private final int committeeId = 1;
        private final String username = "testUser";
        private MemberCreationDto memberCreationDto;
        private Committee committee;
        private Member savedMember;

        @BeforeEach
        void setUp() {
            // Create test data
            TestDataHelper helper = new TestDataHelper();
            committee = helper.getCommittee();

            // Create a valid MemberCreationDto
            memberCreationDto = new MemberCreationDto();
            memberCreationDto.setFirstName("John");
            memberCreationDto.setLastName("Doe");
            memberCreationDto.setPost("Test Post");

            // Create a Member that will be returned by the repository
            savedMember = new Member();
            savedMember.setId(1);
            savedMember.setFirstName("John");
            savedMember.setLastName("Doe");
//            savedMember.setFirstNameNepali("जोन");
//            savedMember.setLastNameNepali("डो");
            savedMember.setPost("Test Post");
        }

        // 1. Test when firstName is blank
        @Test
        @DisplayName("should throw ValidationFailureException when firstName is blank")
        void testFirstNameBlank() {
            // Arrange
            memberCreationDto.setFirstName("");
            doThrow(new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, Mockito.mock(BindingResult.class)))
                .when(entityValidator).validate(memberCreationDto);

            // Act & Assert
            ValidationFailureException ex = assertThrows(ValidationFailureException.class, () -> {
                memberService.saveNewMember(memberCreationDto,username);
            });

            assertThat(ex.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
            verify(entityValidator, times(1)).validate(memberCreationDto);
            verify(memberRepository, never()).save(any(Member.class));
        }

        // 2. Test when post is blank
        @Test
        @DisplayName("should throw ValidationFailureException when post is blank")
        void testPostBlank() {
            // Arrange
            memberCreationDto.setPost("");
            doThrow(new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, Mockito.mock(BindingResult.class)))
                .when(entityValidator).validate(memberCreationDto);

            // Act & Assert
            ValidationFailureException ex = assertThrows(ValidationFailureException.class, () -> {
                memberService.saveNewMember(memberCreationDto, username);
            });

            assertThat(ex.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
            verify(entityValidator, times(1)).validate(memberCreationDto);
            verify(memberRepository, never()).save(any(Member.class));
        }

        // 3. Test when email format is not correct
        @Test
        @DisplayName("should throw ValidationFailureException when email format is incorrect")
        void testEmailFormatIncorrect() {
            // Arrange
            doThrow(new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, Mockito.mock(BindingResult.class)))
                .when(entityValidator).validate(memberCreationDto);

            // Act & Assert
            ValidationFailureException ex = assertThrows(ValidationFailureException.class, () -> {
                memberService.saveNewMember(memberCreationDto, username);
            });

            assertThat(ex.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
            verify(entityValidator, times(1)).validate(memberCreationDto);
            verify(memberRepository, never()).save(any(Member.class));
        }

//        // 4. Test when role is blank
//        @Test
//        @DisplayName("should throw ValidationFailureException when role is blank")
//        void testRoleBlank() {
//            // Arrange
//            doThrow(new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, Mockito.mock(BindingResult.class)))
//                .when(entityValidator).validate(memberCreationDto);
//
//            // Act & Assert
//            ValidationFailureException ex = assertThrows(ValidationFailureException.class, () -> {
//                memberService.saveNewMemberDeprecated(memberCreationDto, committee, username);
//            });
//
//            assertThat(ex.getMessage()).isEqualTo(ExceptionMessages.VALIDATION_FAILED.toString());
//            verify(entityValidator, times(1)).validate(memberCreationDto);
//            verify(memberRepository, never()).save(any(Member.class));
//        }
//
//        // 5. Test when firstNameNepali, lastNameNepali, institution is blank/null
//        @Test
//        @DisplayName("should succeed when firstNameNepali, lastNameNepali, institution are blank/null")
//        void testOptionalFieldsBlank() {
//            // Arrange
//            memberCreationDto.setFirstNameNepali("");
//            memberCreationDto.setLastNameNepali(null);
//            memberCreationDto.setInstitution("");
//            doNothing().when(entityValidator).validate(memberCreationDto);
//
//                // Mock repository save method
//            when(memberRepository.save(any(Member.class))).thenReturn(savedMember);
//
//
//            // Act
//            Member result = memberService.saveNewMemberDeprecated(memberCreationDto, committee, username);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(result.getId()).isEqualTo(savedMember.getId());
//            assertThat(result.getFirstName()).isEqualTo(savedMember.getFirstName());
//            assertThat(result.getLastName()).isEqualTo(savedMember.getLastName());
//
//            verify(entityValidator, times(1)).validate(memberCreationDto);
//            verify(memberRepository, times(1)).save(any(Member.class));
//        }

//        // 6. Test the success case with firstNameNepali, lastNameNepali
//        @Test
//        @DisplayName("should succeed with all fields including firstNameNepali, lastNameNepali")
//        void testSuccessCase() {
//            // Arrange
//            doNothing().when(entityValidator).validate(memberCreationDto);
//                // Mock repository save method
//            when(memberRepository.save(any(Member.class))).thenReturn(savedMember);
//                // Mock RequestContextHolder
//
//            // Act
//            Member result = memberService.saveNewMemberDeprecated(memberCreationDto, committee, username);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(result.getId()).isEqualTo(savedMember.getId());
//            assertThat(result.getFirstName()).isEqualTo(savedMember.getFirstName());
//            assertThat(result.getLastName()).isEqualTo(savedMember.getLastName());
////            assertThat(result.getFirstNameNepali()).isEqualTo(savedMember.getFirstNameNepali());
////            assertThat(result.getLastNameNepali()).isEqualTo(savedMember.getLastNameNepali());
//            assertThat(result.getInstitution()).isEqualTo(savedMember.getInstitution());
//            assertThat(result.getPost()).isEqualTo(savedMember.getPost());
//            assertThat(result.getEmail()).isEqualTo(savedMember.getEmail());
//
//            verify(entityValidator, times(1)).validate(memberCreationDto);
//            verify(memberRepository, times(1)).save(any(Member.class));
//        }
    }


//    @Nested
//    @DisplayName("Tests for the getMemberDetails method")
//    class GetMemberDetails {
//        private final int memberId = 1;
//        private final String username = "testUser";
//        private Member member;
//        //NOTE: committee that is associated with the above member
//        private Committee committee;
//
//        //NOTE: meeting attended by the above member
//        private Meeting meeting;
//
//        @BeforeEach
//        void setUp() {
//            TestDataHelper helper = new TestDataHelper();
//            member = helper.getMember();
//            committee = helper.getCommittee();
//            meeting = helper.getMeeting();
//        }
//
//        @Test
//        @DisplayName("specified memberId is not present in the database")
//        void testMemberNotFound() {
//            // Arrange
//            when(memberRepository.findMemberById(memberId)).thenThrow(new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, memberId));
//
//            // Act & Assert
//            MemberDoesNotExistException ex = assertThrows(MemberDoesNotExistException.class, () -> {
//                memberService.getMemberDetails(memberId, username);
//            });
//
//            assertThat(ex.getMessage()).isEqualTo(ExceptionMessages.MEMBER_DOES_NOT_EXIST.toString());
//
//           verify(memberRepository, times(1)).findMemberById(memberId);
//        }
//
//        @Test
//        @DisplayName("member is not accessible to the user ie the 'createdBy' of the member stored in the database is not same as the supplied username")
//        void testMemberNotAccessible() {
//            // Arrange
//            String differentUsername = "differentUser";
//            member.setCreatedBy(differentUsername);
//            when(memberRepository.findMemberById(memberId)).thenReturn(member);
//
//            // Act & Assert
//            IllegalOperationException ex = assertThrows(IllegalOperationException.class, () -> {
//                memberService.getMemberDetails(memberId, username);
//            });
//
//            Assertions.assertThat(ex.getMessage()).isEqualTo(ExceptionMessages.MEMBER_NOT_ACCESSIBLE.toString());
//
//            verify(memberRepository, times(1)).findMemberById(memberId);
//        }
//
//        @Test
//        @DisplayName("attended meeting for a particular member is null")
//        void testAttendedMeetingsNull() {
//            // Arrange
//            member.setAttendedMeetings(null);
//            when(memberRepository.findMemberById(memberId)).thenReturn(member);
//
//            // Act
//            MemberDetailsDto result = memberService.getMemberDetails(memberId, username);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(memberId).isEqualTo(result.getMemberId());
//            assertThat(member.getFirstName()).isEqualTo(result.getFirstName());
//            assertThat(member.getLastName()).isEqualTo(result.getLastName());
//            assertThat(result.getCommitteeWithMeetings().isEmpty()).isFalse();
//
//            // Verify meeting attendance is false since attendedMeetings is null
//            MemberDetailsDto.MeetingInfo meetingInfo = result.getCommitteeWithMeetings().getFirst().meetingInfos().getFirst();
//            assertThat(meetingInfo.hasAttendedMeeting()).isFalse();
//
//            verify(memberRepository, times(1)).findMemberById(memberId);
//        }
//
//        @Test
//        @DisplayName("committee for a particular member is null")
//        void testCommitteesNull() {
//            // Arrange
//            member.setMemberships(new LinkedList<>());
//            when(memberRepository.findMemberById(memberId)).thenReturn(member);
//
//            // Act
//            MemberDetailsDto result = memberService.getMemberDetails(memberId, username);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(memberId).isEqualTo(result.getMemberId());
//            assertThat(member.getFirstName()).isEqualTo(result.getFirstName());
//            assertThat(member.getLastName()).isEqualTo(result.getLastName());
//            assertThat(result.getCommitteeWithMeetings().isEmpty()).isTrue();
//
//            verify(memberRepository, times(1)).findMemberById(memberId);
//        }
//
//        @Test
//        @DisplayName("success case")
//        void testSuccessCase() {
//            // Arrange
//            when(memberRepository.findMemberById(memberId)).thenReturn(member);
//
//            // Act
//            MemberDetailsDto result = memberService.getMemberDetails(memberId, username);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(memberId).isEqualTo(result.getMemberId());
//            assertThat(member.getFirstName()).isEqualTo(result.getFirstName());
//            assertThat(member.getLastName()).isEqualTo(result.getLastName());
//            assertThat(member.getInstitution()).isEqualTo(result.getInstitution());
//            assertThat(member.getPost()).isEqualTo(result.getPost());
////            assertThat(member.getQualification()).isEqualTo(result.getQualification());
//
//            // Check committee info
//            assertThat(result.getCommitteeWithMeetings().isEmpty()).isFalse();
//            MemberDetailsDto.CommitteeInfo committeeInfo = result.getCommitteeWithMeetings().getFirst().committeeInfo();
//            assertThat(committeeInfo.id()).isEqualTo(1);
//            assertThat(committee.getName()).isEqualTo(committeeInfo.committeeName());
//            assertThat(committee.getDescription()).isEqualTo(committeeInfo.committeeDescription());
//            assertThat("Member").isEqualTo(committeeInfo.role());
//
//            // Check meeting info
//            assertThat(result.getCommitteeWithMeetings().getFirst().meetingInfos().isEmpty()).isFalse();
//            MemberDetailsDto.MeetingInfo meetingInfo = result.getCommitteeWithMeetings().getFirst().meetingInfos().getFirst();
//            assertThat(meetingInfo.id()).isEqualTo(1);
//            assertThat(meeting.getTitle()).isEqualTo(meetingInfo.meetingTitle());
//            assertThat(meeting.getDescription()).isEqualTo(meetingInfo.meetingDescription());
//            assertThat(meetingInfo.hasAttendedMeeting()).isTrue();
//
//            verify(memberRepository, times(1)).findMemberById(memberId);
//        }
//    }
}
