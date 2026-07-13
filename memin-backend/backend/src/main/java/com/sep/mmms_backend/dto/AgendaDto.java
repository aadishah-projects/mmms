package com.sep.mmms_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep.mmms_backend.entity.Agenda;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AgendaDto {
    private final Integer agendaId;

    @NotBlank(message = "agenda can't be empty")
    private final String agenda;

    public AgendaDto(Agenda agenda) {
        this.agendaId = agenda.getAgendaId();
        this.agenda = agenda.getAgenda();
    }


    @JsonCreator
    public AgendaDto(
            @JsonProperty("agendaId") Integer agendaId,
            @JsonProperty("agenda") String agenda) {
        this.agendaId = agendaId;
        this.agenda = agenda;
    }
}
