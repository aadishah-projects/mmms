package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.AiProviderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiConfigurationDto {
    private boolean enabled;
    private AiProviderType provider;
    private String baseUrl;
    private String model;
    private int maxTokens;
    private String additionalInstructions;
    private boolean apiKeyConfigured;
    private String maskedApiKey;
    private String source;
}
