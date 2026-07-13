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
    String committeeName;
    String committeeDescription;
    String coordinatorFullName;
    List<DecisionDto> decisions;
    List<AgendaDto> agendas;
    List<CommitteeMembershipDto> participants;
}
