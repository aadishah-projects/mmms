package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.global_constants.ValidationErrorMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Getter
@Setter
public class MeetingCreationDto {
    @Positive(message = "Committee ID should be positive")
    private Integer committeeId;

    /** The member chairing this particular meeting. Null means committee coordinator. */
    private Integer chairmanId;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String title;

    @NotNull(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private LocalDate heldDate;

    @NotNull(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.sep.mmms_backend.util.NepaliLocalTimeDeserializer.class)
    private LocalTime heldTime;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String heldPlace;

    private LinkedHashSet<Integer> inviteeIds = new LinkedHashSet<>();

    private List<DecisionDto> decisions = new ArrayList<>();

    private List<AgendaDto> agendas = new ArrayList<>();
}
