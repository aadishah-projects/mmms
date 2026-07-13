package com.sep.mmms_backend.aop;

import com.sep.mmms_backend.aop.implementations.CheckCommitteeAccessAspect;
import com.sep.mmms_backend.aop.interfaces.CheckCommitteeAccess;
import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.exceptions.CommitteeNotAccessibleException;
import com.sep.mmms_backend.exceptions.ExceptionMessages;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.exceptions.MeetingNotAccessibleException;
import com.sep.mmms_backend.testing_tools.TestDataHelper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CheckCommitteeAccessTests {
   /*
        Perform unit tests for CheckCommmitteeAccessAspect method
        Create/Mock the join point with Committee and Meeting

        1. test when checkCommitteeAccess.shouldValidateMeeting = false
            test when the committee does not exist
            test when the committee is not accessible
        2. test when checkCommitteeAccess.shouldValidateMeeting = true(validate both)
            test when the meeting does not exist
            test when the meeting is not accessiblell
    */


    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private CheckCommitteeAccess checkCommitteeAccess;


    @InjectMocks
    private CheckCommitteeAccessAspect aspect;
    private final String username = "testUser";

    private Committee committee;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        // Create test data
        TestDataHelper helper = new TestDataHelper();
        committee = helper.getCommittee();
        meeting = helper.getMeeting();

        // Set up joinPoint
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Nested
    @DisplayName("Tests when shouldValidateMeeting = false")
    class WhenShouldValidateMeetingIsFalse {

        @BeforeEach
        void setUp() {
            // Set up method signature
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"committee", "username"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{committee, username});

            // Set shouldValidateMeeting to false
            when(checkCommitteeAccess.shouldValidateMeeting()).thenReturn(false);
        }


        @Test
        @DisplayName("Should throw CommitteeNotAccessibleException when committee is not accessible")
        void testCommitteeNotAccessible() {
            // Arrange
            AppUser differentUser = new AppUser();
            differentUser.setUsername("differentUser");
            committee.setCreatedBy(differentUser.getUsername());

            // Act & Assert
            assertThatThrownBy(() -> aspect.checkCommitteeAccess(joinPoint, checkCommitteeAccess))
                    .isInstanceOf(CommitteeNotAccessibleException.class)
                    .hasMessageContaining(ExceptionMessages.COMMITTEE_NOT_ACCESSIBLE.toString());
        }

        @Test
        @DisplayName("Should not throw exception when committee exists and is accessible")
        void testCommitteeExistsAndIsAccessible() {
            // Act & Assert
            assertThatCode(() -> aspect.checkCommitteeAccess(joinPoint, checkCommitteeAccess))
                    .doesNotThrowAnyException();
        }

    }


    @Nested
    @DisplayName("Tests when shouldValidateMeeting = true")
    class WhenShouldValidateMeetingIsTrue {

        @BeforeEach
        void setUp() {
            // Set up method signature
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"committee", "username", "meeting"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{committee, username, meeting});

            // Set shouldValidateMeeting to true
            when(checkCommitteeAccess.shouldValidateMeeting()).thenReturn(true);
        }


        @Test
        @DisplayName("Should throw MeetingNotAccessible when meeting is not accessible")
        void testCommitteeNotAccessible() {
            // Arrange
            meeting.setCreatedBy("differentUser");

            // Act & Assert
            assertThatThrownBy(() -> aspect.checkCommitteeAccess(joinPoint, checkCommitteeAccess))
                    .isInstanceOf(MeetingNotAccessibleException.class)
                    .hasMessageContaining(ExceptionMessages.MEETING_NOT_ACCESSIBLE.toString());
        }


        @Test
        @DisplayName("Should not throw exception when meeting exists and is in committee")
        void testMeetingExistsAndIsInCommittee() {

            // Act & Assert
            assertThatCode(() -> aspect.checkCommitteeAccess(joinPoint, checkCommitteeAccess))
                    .doesNotThrowAnyException();
        }
    }
}
