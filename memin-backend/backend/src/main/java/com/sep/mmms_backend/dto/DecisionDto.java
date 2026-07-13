package com.sep.mmms_backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep.mmms_backend.entity.Decision;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class DecisionDto {
    private final Integer decisionId;

    @NotBlank(message = "decision can't be empty")
    private final String decision;

    public DecisionDto(Decision decision) {
        this.decisionId = decision.getDecisionId();
        this.decision = decision.getDecision();
    }

    @JsonCreator
    public DecisionDto(
            @JsonProperty("decisionId") Integer decisionId,
            @JsonProperty("decision") String decision) {
        this.decisionId = decisionId;
        this.decision = decision;
    }
}
