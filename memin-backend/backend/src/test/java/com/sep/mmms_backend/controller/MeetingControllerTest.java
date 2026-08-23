package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.MeetingCreationDto;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.service.CommitteeService;
import com.sep.mmms_backend.service.MeetingMinutePreparationService;
import com.sep.mmms_backend.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MeetingControllerTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private CommitteeService committeeService;

    @Mock
    private MeetingMinutePreparationService meetingMinutePreparationService;

    @Mock
    private Authentication authentication;

    @Test
    void doesNotReplaceSavedEditedMinuteWhenMeetingEditChangesParticipantOrder() {
        Meeting meeting = new Meeting();
        meeting.setMinuteContentHtml("<p>edited current minute</p>");
        meeting.setMinuteTemplateHtml("<p>old frozen template</p>");

        when(authentication.getName()).thenReturn("writer");
        when(meetingService.updateExistingMeeting(any(MeetingCreationDto.class), eq(20), eq("writer")))
                .thenReturn(meeting);

        MeetingController controller = new MeetingController(
                meetingService,
                committeeService,
                meetingMinutePreparationService);

        controller.updateMeeting(new MeetingCreationDto(), 20, authentication);

        verify(meetingMinutePreparationService, never())
                .prepareDataForMinute(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString());
        verify(meetingService, never())
                .updateMinuteContent(org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
    }
}
