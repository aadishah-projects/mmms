package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.*;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import com.sep.mmms_backend.service.CommitteeService;
import com.sep.mmms_backend.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("api")
public class MeetingController {
    private final MeetingService meetingService;
    private final CommitteeService committeeService;
    private final com.sep.mmms_backend.service.MeetingMinutePreparationService meetingMinutePreparationService;

    MeetingController(MeetingService meetingService, CommitteeService committeeService, com.sep.mmms_backend.service.MeetingMinutePreparationService meetingMinutePreparationService) {
        this.meetingService = meetingService;
        this.committeeService = committeeService;
        this.meetingMinutePreparationService = meetingMinutePreparationService;
    }


    //TODO: Create Tests
    @PostMapping("/meeting")
    public ResponseEntity<Response> createMeeting(
            @RequestBody(required = true) MeetingCreationDto meetingCreationDto,
            Authentication authentication) {
        Committee committee = committeeService.findCommitteeById(meetingCreationDto.getCommitteeId());
        Meeting savedMeeting = meetingService.saveNewMeeting(meetingCreationDto, committee, authentication.getName());
        MeetingSummaryDto savedMeetingSummary = new MeetingSummaryDto(savedMeeting);
        return ResponseEntity.ok(new Response(ResponseMessages.MEETING_CREATION_SUCCESSFUL, savedMeetingSummary));
    }


    @PatchMapping("/minute")
    public ResponseEntity<Response> createMeeting(@RequestBody MinuteUpdationDto meetingUpdationDto, @RequestParam int committeeId, @RequestParam int meetingId, Authentication authentication) {
        meetingService.updateExistingMeetingMinute(meetingUpdationDto, meetingId, committeeId, authentication.getName());

        return ResponseEntity.ok(new Response(ResponseMessages.MEETING_UPDATION_SUCCESS));
    }

    @GetMapping("/meetings-of-committee")
    public ResponseEntity<Response> getMeetingsOfCommittee(@RequestParam(required = true) int committeeId, Authentication authentication) {
        Committee committee = committeeService.findCommitteeById(committeeId);

        List<MeetingSummaryDto> meetings = meetingService.getMeetingOfCommittee(committee, authentication.getName());

        return ResponseEntity.ok(new Response(ResponseMessages.MEETINGS_OF_COMMITTEE, meetings));
    }

    @GetMapping("meeting-details-for-edit")
    public ResponseEntity<Response> getMeetingDetailsForEdit(@RequestParam int meetingId, Authentication authentication) {
        MeetingDetailsForEditDto meetingDetailsForEditDto = meetingService.getMeetingDetails(meetingId, authentication.getName());
        return ResponseEntity.ok(new Response("Requested meeting details: ", meetingDetailsForEditDto));
    }

    @PatchMapping("meeting")
    public ResponseEntity<Response> updateMeeting(@RequestBody MeetingCreationDto meetingCreationDto, @RequestParam Integer meetingId, Authentication authentication) {
       Meeting meeting = meetingService.updateExistingMeeting(meetingCreationDto, meetingId, authentication.getName());
       
       // A saved minute may contain direct edits or AI-generated content. Do
       // not replace it with the meeting's frozen template just because the
       // participant order changed in the edit form. Only render a template
       // when this meeting does not yet have saved minute HTML.
       boolean hasSavedMinute = meeting.getMinuteContentHtml() != null
               && !meeting.getMinuteContentHtml().isBlank();
       if (!hasSavedMinute && hasMeetingTemplate(meeting)) {
           MinuteDataDto updatedData = meetingMinutePreparationService.prepareDataForMinute(
                   meeting.getCommittee(), meeting, authentication.getName());
           String htmlContent = meetingMinutePreparationService.renderMeetingTemplate(
                   meeting, updatedData);
           meetingService.updateMinuteContent(meetingId, htmlContent, authentication.getName());
       }

       return ResponseEntity.ok(new Response(ResponseMessages.MEETING_UPDATION_SUCCESS));
    }

    @PatchMapping("/meeting/{meetingId}/participant-order")
    public ResponseEntity<Response> updateParticipantOrder(
            @PathVariable int meetingId,
            @RequestBody ParticipantOrderUpdateDto request,
            Authentication authentication) {
        Meeting updatedMeeting = meetingService.updateParticipantOrder(
                meetingId,
                request == null ? List.of() : request.getParticipantIds(),
                authentication.getName());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("participantIds", updatedMeeting.getAttendees().stream().map(Member::getId).toList());
        String minuteContentHtml = updatedMeeting.getMinuteContentHtml();
        if (minuteContentHtml == null && hasMeetingTemplate(updatedMeeting)) {
            MinuteDataDto minuteData = meetingMinutePreparationService.prepareDataForMinute(
                    updatedMeeting.getCommittee(), updatedMeeting, authentication.getName());
            minuteContentHtml = meetingMinutePreparationService.renderMeetingTemplate(
                    updatedMeeting, minuteData);
        }
        // Return a rendered preview for snapshot templates, but do not persist
        // it here. Reordering participants must never create a new minute table
        // or replace a template that deliberately has no attendance section.
        result.put("minuteContentHtml", minuteContentHtml);
        return ResponseEntity.ok(new Response("Participant order saved", result));
    }

    private boolean hasMeetingTemplate(Meeting meeting) {
        String snapshot = meeting.getMinuteTemplateHtml();
        if (snapshot != null) {
            return !snapshot.isBlank();
        }
        String legacyTemplate = meeting.getCommittee().getMinuteTemplateHtml();
        return legacyTemplate != null && !legacyTemplate.isBlank();
    }

    @DeleteMapping("/meeting/{meetingId}")
    public ResponseEntity<Response> deleteMeeting(@PathVariable Integer meetingId, Authentication authentication) {
        meetingService.deleteMeeting(meetingId, authentication.getName());
        return ResponseEntity.ok(new Response(ResponseMessages.MEETING_DELETED_SUCCESSFULLY));
    }

    @PostMapping("/meeting/{meetingId}/send-invites")
    public ResponseEntity<Response> sendMeetingInvites(@PathVariable Integer meetingId, Authentication authentication) {
        int sentCount = meetingService.sendMeetingInvites(meetingId, authentication.getName());
        return ResponseEntity.ok(new Response("Final meeting minutes sent", sentCount));
    }

}
