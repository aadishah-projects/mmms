package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.global_constants.ValidationErrorMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class MinuteUpdationDto {
    @NotBlank(message = "committee name is required")
    String committeeName;

    @NotBlank(message = "committee description/aim is required")
    String committeeDescription;

    @NotNull(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    LocalDate meetingHeldDate;

    @NotNull(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    LocalTime meetingHeldTime;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    String meetingHeldPlace;

    List<DecisionDto> decisions;
    List<AgendaDto> agendas;
}
