package com.sep.mmms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * The AI assistant is intentionally limited to the structured parts of a
 * minute. The final document is always rendered by the application template.
 */
@Getter
@AllArgsConstructor
public class AiStructuredMinuteDto {
    private final List<AgendaDto> agendas;
    private final List<DecisionDto> decisions;
}
