package com.sep.mmms_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sep.mmms_backend.enums.MinuteLanguage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MinuteDataDto {
    MinuteLanguage minuteLanguage;
    String meetingHeldDateNepali;

    //frontend needs this format so that <input type="date"/> can use it
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate meetingHeldDate;
    String meetingHeldDay;
    String partOfDay;
    String meetingHeldTime;
    String meetingHeldPlace;
    String meetingTitle;
    String committeeName;
    String committeeDescription;
    String coordinatorFullName;
    String chairmanFullName;
    // Resolved per-committee opening paragraph (placeholders substituted). Null when the
    // committee has no custom template — the frontend then renders its built-in default.
    String openingParagraph;
    // Resolved per-committee header/letterhead (placeholders substituted). Null when unset.
    String header;
    // Resolved committee template or meeting-specific HTML override. Null means
    // the frontend should use its built-in language template.
    String minuteContentHtml;
    List<DecisionDto> decisions;
    List<AgendaDto> agendas;
    List<CommitteeMembershipDto> participants;
}
