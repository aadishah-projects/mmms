package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.*;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.response.ResponseMessages;
import com.sep.mmms_backend.service.CommitteeService;
import com.sep.mmms_backend.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("api")
public class MeetingController {
    private final MeetingService meetingService;
    private final CommitteeService committeeService;

    MeetingController(MeetingService meetingService, CommitteeService committeeService) {
        this.meetingService = meetingService;
        this.committeeService = committeeService;
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
       return ResponseEntity.ok(new Response(ResponseMessages.MEETING_UPDATION_SUCCESS));
    }

}
