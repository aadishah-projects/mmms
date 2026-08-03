package com.sep.mmms_backend.controller;

import com.sep.mmms_backend.dto.MinuteDataDto;
import com.sep.mmms_backend.dto.AiMinuteRequestDto;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.response.Response;
import com.sep.mmms_backend.service.CommitteeService;
import com.sep.mmms_backend.service.MeetingMinutePreparationService;
import com.sep.mmms_backend.service.MeetingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class MeetingMinuteController {
    private final MeetingMinutePreparationService meetingMinutePreparationService;
    private final MeetingService meetingService;
    private final com.sep.mmms_backend.service.AiMinuteService aiMinuteService;
    public MeetingMinuteController(MeetingMinutePreparationService meetingMinutePreparationService, MeetingService meetingService, com.sep.mmms_backend.service.AiMinuteService aiMinuteService) {
        this.meetingMinutePreparationService = meetingMinutePreparationService;
        this.meetingService = meetingService;
        this.aiMinuteService = aiMinuteService;
    }

    // Read the meeting and build the minute data in one transaction so the meeting's
    // (and committee's) lazy collections — agendas, decisions, invitees, memberships —
    // can initialize while the DTO is assembled. The app runs with
    // spring.jpa.open-in-view=false, so there is otherwise no session open here.
    @Transactional(readOnly = true)
    @GetMapping("api/data-for-minute")
    public ResponseEntity<Response> getDataForMinute( @RequestParam int meetingId, Authentication authentication) {
        Meeting meeting =  meetingService.findMeetingById(meetingId);
        Committee committee = meeting.getCommittee();

        MinuteDataDto minuteData = this.meetingMinutePreparationService.prepareDataForMinute(committee, meeting, authentication.getName());

        return ResponseEntity.ok(new Response("Meeting Minute Data: ", minuteData));
    }

    @PostMapping("api/meetings/{meetingId}/ai-minute")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Response> generateAiMinute(
            @org.springframework.web.bind.annotation.PathVariable int meetingId,
            @jakarta.validation.Valid @RequestBody AiMinuteRequestDto request,
            Authentication authentication) {
        Meeting meeting = meetingService.findMeetingById(meetingId);
        MinuteDataDto minuteData = meetingMinutePreparationService.prepareDataForMinute(
                meeting.getCommittee(), meeting, authentication.getName());
        String htmlContent = aiMinuteService.generateMinute(minuteData, request.getRoughPrompt());
        meetingService.updateMinuteContent(meetingId, htmlContent, authentication.getName());
        return ResponseEntity.ok(new Response("AI minute draft generated", java.util.Map.of("htmlContent", htmlContent)));
    }

    @PostMapping("api/word-file-for-minute")
    public ResponseEntity<?> getWordFileForMinute(@RequestBody String htmlContent, Authentication authentication) {
            byte[] docxBytes;
            try {
                docxBytes = meetingMinutePreparationService.createWordDocumentFromHtml(htmlContent);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(e.getMessage());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            String filename = "MeetingMinutes_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".docx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(docxBytes, headers, HttpStatus.OK);
    }
}
