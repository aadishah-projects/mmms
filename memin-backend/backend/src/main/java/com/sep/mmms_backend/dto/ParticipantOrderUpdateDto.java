package com.sep.mmms_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ParticipantOrderUpdateDto {
    @NotNull
    private List<Integer> participantIds = new ArrayList<>();
}
