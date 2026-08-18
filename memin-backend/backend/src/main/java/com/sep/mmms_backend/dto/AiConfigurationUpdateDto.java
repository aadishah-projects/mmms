package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.AiProviderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiConfigurationUpdateDto {
    private Boolean enabled;
    private AiProviderType provider;
    private String baseUrl;
    /** Optional. Blank means retain the saved key. */
    private String apiKey;
    private String model;
    private Integer maxTokens;
    private String additionalInstructions;
}
