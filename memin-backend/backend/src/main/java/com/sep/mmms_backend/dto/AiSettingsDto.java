package com.sep.mmms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiSettingsDto {
    private String providerType; // "ANTHROPIC", "OPENAI_COMPATIBLE", or "OPENAI_RESPONSES"
    private String baseUrl;
    private String apiKey;
    private boolean hasApiKey;
    private String model;
    private Integer maxTokens;
}
