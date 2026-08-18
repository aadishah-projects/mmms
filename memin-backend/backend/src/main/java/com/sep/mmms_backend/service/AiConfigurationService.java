package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.AiConfigurationDto;
import com.sep.mmms_backend.dto.AiConfigurationUpdateDto;
import com.sep.mmms_backend.entity.AiConfiguration;
import com.sep.mmms_backend.enums.AiProviderType;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.repository.AiConfigurationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AiConfigurationService {
    private final AiConfigurationRepository repository;
    private final AiSecretCipher secretCipher;

    @Value("${llm.enabled:true}")
    private boolean environmentEnabled;

    @Value("${llm.provider:ANTHROPIC_COMPATIBLE}")
    private String environmentProvider;

    @Value("${llm.base-url:}")
    private String environmentBaseUrl;

    @Value("${llm.api-key:}")
    private String environmentApiKey;

    @Value("${llm.model:mimo-v2.5-pro}")
    private String environmentModel;

    @Value("${llm.max-tokens:2500}")
    private int environmentMaxTokens;

    @Value("${llm.additional-instructions:}")
    private String environmentAdditionalInstructions;

    public AiConfigurationService(
            AiConfigurationRepository repository,
            AiSecretCipher secretCipher) {
        this.repository = repository;
        this.secretCipher = secretCipher;
    }

    @Transactional
    public AiConfigurationDto getConfiguration() {
        return toDto(repository.findById(AiConfiguration.SINGLETON_ID).orElse(null));
    }

    @Transactional
    public void updateConfiguration(AiConfigurationUpdateDto request, String username) {
        if (request == null) {
            throw new IllegalOperationException("AI configuration is required");
        }

        AiConfiguration current = repository.findById(AiConfiguration.SINGLETON_ID).orElse(null);
        ActiveAiConfiguration effectiveBeforeSave = current == null
                ? environmentConfiguration()
                : resolve(current);

        boolean enabled = request.getEnabled() == null
                ? current == null ? environmentEnabled : current.isEnabled()
                : request.getEnabled();
        AiProviderType provider = request.getProvider() == null
                ? parseProvider(current == null || current.getProvider() == null
                ? environmentProvider
                : current.getProvider().name())
                : request.getProvider();
        String baseUrl = normalize(request.getBaseUrl());
        if (baseUrl == null) {
            baseUrl = effectiveBeforeSave.baseUrl();
        }
        String model = normalize(request.getModel());
        if (model == null) {
            model = effectiveBeforeSave.model();
        }
        int maxTokens = request.getMaxTokens() == null
                ? effectiveBeforeSave.maxTokens()
                : request.getMaxTokens();
        validateMaxTokens(maxTokens);

        String requestedApiKey = normalize(request.getApiKey());
        String effectiveApiKey = requestedApiKey;
        if (effectiveApiKey == null) {
            effectiveApiKey = effectiveBeforeSave.apiKey();
        }

        if (enabled && (baseUrl == null || model == null || effectiveApiKey == null)) {
            throw new IllegalOperationException(
                    "Enabled AI configuration requires a base URL, model, and API key");
        }
        validateBaseUrl(baseUrl);

        AiConfiguration configuration = current == null ? new AiConfiguration() : current;
        configuration.setId(AiConfiguration.SINGLETON_ID);
        configuration.setEnabled(enabled);
        configuration.setProvider(provider);
        configuration.setBaseUrl(baseUrl);
        configuration.setModel(model);
        configuration.setMaxTokens(maxTokens);
        configuration.setAdditionalInstructions(normalize(request.getAdditionalInstructions()));
        configuration.setUpdatedBy(username);
        configuration.setUpdatedDate(LocalDate.now());
        if (requestedApiKey != null) {
            configuration.setEncryptedApiKey(secretCipher.encrypt(requestedApiKey));
        }
        repository.save(configuration);
    }

    @Transactional
    public void clearSavedConfiguration() {
        repository.deleteById(AiConfiguration.SINGLETON_ID);
    }

    @Transactional
    public ActiveAiConfiguration getActiveConfiguration() {
        return repository.findById(AiConfiguration.SINGLETON_ID)
                .map(this::resolve)
                .orElseGet(this::environmentConfiguration);
    }

    /** Builds a request-only configuration for the connection test. */
    @Transactional
    public ActiveAiConfiguration preview(AiConfigurationUpdateDto request) {
        if (request == null) {
            throw new IllegalOperationException("AI configuration is required");
        }
        ActiveAiConfiguration current = getActiveConfiguration();
        boolean enabled = request.getEnabled() == null ? current.enabled() : request.getEnabled();
        AiProviderType provider = request.getProvider() == null ? current.provider() : request.getProvider();
        String baseUrl = normalize(request.getBaseUrl());
        String apiKey = normalize(request.getApiKey());
        String model = normalize(request.getModel());
        int maxTokens = request.getMaxTokens() == null ? current.maxTokens() : request.getMaxTokens();
        String instructions = normalize(request.getAdditionalInstructions());

        validateMaxTokens(maxTokens);
        if (baseUrl == null) baseUrl = current.baseUrl();
        if (apiKey == null) apiKey = current.apiKey();
        if (model == null) model = current.model();
        if (instructions == null) instructions = current.additionalInstructions();
        validateBaseUrl(baseUrl);

        return new ActiveAiConfiguration(
                enabled, provider, baseUrl, apiKey, model, maxTokens, instructions, "PREVIEW");
    }

    private AiConfigurationDto toDto(AiConfiguration saved) {
        ActiveAiConfiguration configuration = saved == null
                ? environmentConfiguration()
                : resolve(saved);
        boolean hasKey = configuration.apiKey() != null && !configuration.apiKey().isBlank();
        String source = saved == null
                ? (hasEnvironmentConfiguration() ? "ENVIRONMENT" : "NONE")
                : "DATABASE";
        return new AiConfigurationDto(
                configuration.enabled(),
                configuration.provider(),
                configuration.baseUrl(),
                configuration.model(),
                configuration.maxTokens(),
                configuration.additionalInstructions(),
                hasKey,
                hasKey ? mask(configuration.apiKey()) : null,
                source);
    }

    private ActiveAiConfiguration resolve(AiConfiguration saved) {
        String apiKey = normalize(saved.getEncryptedApiKey()) == null
                ? normalize(environmentApiKey)
                : secretCipher.decrypt(saved.getEncryptedApiKey());
        return new ActiveAiConfiguration(
                saved.isEnabled(),
                saved.getProvider() == null ? parseProvider(environmentProvider) : saved.getProvider(),
                valueOr(saved.getBaseUrl(), environmentBaseUrl),
                apiKey,
                valueOr(saved.getModel(), environmentModel),
                saved.getMaxTokens() == null ? safeEnvironmentMaxTokens() : saved.getMaxTokens(),
                valueOr(saved.getAdditionalInstructions(), environmentAdditionalInstructions),
                "DATABASE");
    }

    private ActiveAiConfiguration environmentConfiguration() {
        return new ActiveAiConfiguration(
                environmentEnabled,
                parseProvider(environmentProvider),
                normalize(environmentBaseUrl),
                normalize(environmentApiKey),
                valueOr(environmentModel, "mimo-v2.5-pro"),
                safeEnvironmentMaxTokens(),
                normalize(environmentAdditionalInstructions),
                "ENVIRONMENT");
    }

    private boolean hasEnvironmentConfiguration() {
        return normalize(environmentBaseUrl) != null || normalize(environmentApiKey) != null;
    }

    private int safeEnvironmentMaxTokens() {
        return Math.max(500, Math.min(100_000, environmentMaxTokens));
    }

    private void validateMaxTokens(int maxTokens) {
        if (maxTokens < 500 || maxTokens > 100_000) {
            throw new IllegalOperationException("Maximum output tokens must be between 500 and 100000");
        }
    }

    private void validateBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return;
        }
        if (!(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))) {
            throw new IllegalOperationException("AI base URL must start with http:// or https://");
        }
    }

    private AiProviderType parseProvider(String value) {
        try {
            return AiProviderType.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return AiProviderType.ANTHROPIC_COMPATIBLE;
        }
    }

    private String valueOr(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? normalize(fallback) : normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String mask(String value) {
        if (value.length() <= 4) {
            return "••••";
        }
        return "••••" + value.substring(value.length() - 4);
    }

    public record ActiveAiConfiguration(
            boolean enabled,
            AiProviderType provider,
            String baseUrl,
            String apiKey,
            String model,
            int maxTokens,
            String additionalInstructions,
            String source) {

        public boolean configured() {
            return enabled
                    && baseUrl != null && !baseUrl.isBlank()
                    && apiKey != null && !apiKey.isBlank()
                    && model != null && !model.isBlank();
        }
    }
}
