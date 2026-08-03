package com.sep.mmms_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiMinuteRequestDto {
    @NotBlank(message = "Describe what the minute should contain")
    private String roughPrompt;
}
