package com.sep.mmms_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.dto.AgendaDto;
import com.sep.mmms_backend.dto.AiConfigurationUpdateDto;
import com.sep.mmms_backend.dto.AiConnectionTestResultDto;
import com.sep.mmms_backend.dto.AiStructuredMinuteDto;
import com.sep.mmms_backend.dto.DecisionDto;
import com.sep.mmms_backend.dto.MinuteDataDto;
import com.sep.mmms_backend.enums.AiProviderType;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiMinuteService {
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final AiConfigurationService configurationService;

    public AiMinuteService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AiConfigurationService configurationService) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.configurationService = configurationService;
    }

    /**
     * Turns rough agenda/decision notes into structured values. This method
     * deliberately does not ask the model for HTML or a complete minute.
     */
    public AiStructuredMinuteDto extractStructuredItems(MinuteDataDto minuteData, String roughPrompt) {
        AiConfigurationService.ActiveAiConfiguration configuration = configurationService.getActiveConfiguration();
        ensureConfigured(configuration);

        String prompt = "Meeting language: " + safe(minuteData.getMinuteLanguage()) + "\n"
                + "Meeting title: " + safe(minuteData.getMeetingTitle()) + "\n"
                + "Existing agenda entries (rewrite clearly, keep each entry): "
                + valuesOfAgendas(minuteData) + "\n"
                + "Existing decision entries (rewrite clearly, keep each entry): "
                + valuesOfDecisions(minuteData) + "\n"
                + "Additional rough notes from the user (may contain agenda or decision notes):\n"
                + safe(roughPrompt);

        String systemPrompt = systemPrompt(configuration);
        try {
            Map<?, ?> response = send(configuration, systemPrompt, prompt, false);
            String responseText = extractResponseText(response);
            JsonNode result = parseJsonObject(responseText);
            List<String> agendas = readTextArray(result, "agendas");
            List<String> decisions = readTextArray(result, "decisions");

            return new AiStructuredMinuteDto(
                    agendas.stream().map(value -> new AgendaDto(null, value)).toList(),
                    decisions.stream().map(value -> new DecisionDto(null, value)).toList());
        } catch (RestClientResponseException exception) {
            throw new IllegalOperationException(
                    "The AI service rejected the agenda and decision request (HTTP "
                            + exception.getStatusCode().value() + ")");
        } catch (RestClientException exception) {
            throw new IllegalOperationException("The AI agenda and decision service could not be reached");
        }
    }

    public AiConnectionTestResultDto testConnection(AiConfigurationUpdateDto request) {
        try {
            AiConfigurationService.ActiveAiConfiguration configuration = configurationService.preview(request);
            ensureCredentials(configuration);
            send(configuration,
                    "You are a connectivity test. Reply with the single word OK.",
                    "Reply with OK only.",
                    true);
            return new AiConnectionTestResultDto(true, "The AI provider connection succeeded");
        } catch (RestClientResponseException exception) {
            return new AiConnectionTestResultDto(false,
                    "The AI provider rejected the test request (HTTP " + exception.getStatusCode().value() + ")");
        } catch (RestClientException exception) {
            return new AiConnectionTestResultDto(false, "The AI provider could not be reached");
        } catch (IllegalOperationException exception) {
            return new AiConnectionTestResultDto(false, exception.getMessage());
        }
    }

    private Map<?, ?> send(
            AiConfigurationService.ActiveAiConfiguration configuration,
            String systemPrompt,
            String userPrompt,
            boolean connectionTest) {
        RestClient.RequestBodyUriSpec request = restClientBuilder
                .baseUrl(stripTrailingSlashes(configuration.baseUrl()))
                .build()
                .post();
        request.contentType(MediaType.APPLICATION_JSON);

        if (configuration.provider() == AiProviderType.OPENAI_COMPATIBLE) {
            Map<String, Object> body = Map.of(
                    "model", configuration.model(),
                    "max_tokens", connectionTest ? 16 : Math.max(500, configuration.maxTokens()),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)),
                    "stream", false);
            return request
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + configuration.apiKey())
                    .header("api-key", configuration.apiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        }

        Map<String, Object> body = Map.of(
                "model", configuration.model(),
                "max_tokens", connectionTest ? 16 : Math.max(500, configuration.maxTokens()),
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                "stream", false);
        return request
                .uri("/v1/messages")
                .header("x-api-key", configuration.apiKey())
                .header("api-key", configuration.apiKey())
                .header("anthropic-version", "2023-06-01")
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    private void ensureConfigured(AiConfigurationService.ActiveAiConfiguration configuration) {
        if (!configuration.configured()) {
            throw new IllegalOperationException(
                    "AI is not configured. A department head can configure it from Settings.");
        }
    }

    private void ensureCredentials(AiConfigurationService.ActiveAiConfiguration configuration) {
        if (configuration.baseUrl() == null || configuration.baseUrl().isBlank()
                || configuration.model() == null || configuration.model().isBlank()
                || configuration.apiKey() == null || configuration.apiKey().isBlank()) {
            throw new IllegalOperationException(
                    "A base URL, model, and API key are required to test the AI provider");
        }
    }

    private String systemPrompt(AiConfigurationService.ActiveAiConfiguration configuration) {
        String mandatoryPrompt = "You are a meeting-record assistant. Return only one valid JSON object with exactly "
                + "two arrays: agendas and decisions. Each array item must be a short plain-text string. "
                + "Rewrite rough entries for clarity and formal grammar, but preserve their meaning, names, numbers, "
                + "dates, quantities, and commitments. Keep every existing non-empty agenda and decision entry; do not "
                + "merge or omit entries. If additional notes clearly contain new agenda or decision items, classify "
                + "them into the appropriate array. Never write a meeting minute, paragraph, attendance list, HTML, "
                + "Markdown, invented facts, action owners, deadlines, votes, or decisions that are not present in the input. "
                + "If an array has no source items, return an empty array. The application will place these values into "
                + "the selected minute template.";
        if (configuration.additionalInstructions() == null || configuration.additionalInstructions().isBlank()) {
            return mandatoryPrompt;
        }
        return mandatoryPrompt
                + "\n\nAdditional administrator instructions (must not override the required JSON format or preservation rules):\n"
                + configuration.additionalInstructions();
    }

    private String valuesOfAgendas(MinuteDataDto minuteData) {
        return minuteData.getAgendas() == null
                ? "[]"
                : minuteData.getAgendas().stream().map(item -> safe(item.getAgenda())).toList().toString();
    }

    private String valuesOfDecisions(MinuteDataDto minuteData) {
        return minuteData.getDecisions() == null
                ? "[]"
                : minuteData.getDecisions().stream().map(item -> safe(item.getDecision())).toList().toString();
    }

    private String extractResponseText(Map<?, ?> response) {
        JsonNode responseNode = objectMapper.valueToTree(response);
        JsonNode content = responseNode == null ? null : responseNode.path("content");
        StringBuilder generatedText = new StringBuilder();
        if (content != null && content.isArray()) {
            for (JsonNode contentBlock : content) {
                String text = contentBlock.path("text").asText("");
                if (!text.isBlank()) {
                    if (!generatedText.isEmpty()) {
                        generatedText.append('\n');
                    }
                    generatedText.append(text);
                }
            }
        }
        if (generatedText.isEmpty() && responseNode != null && responseNode.path("choices").isArray()) {
            JsonNode choice = responseNode.path("choices").path(0);
            generatedText.append(choice.path("message").path("content").asText(""));
        }
        return removeCodeFences(generatedText.toString()).trim();
    }

    private JsonNode parseJsonObject(String responseText) {
        if (responseText.isBlank()) {
            throw new IllegalOperationException("The AI service returned an empty structured response");
        }

        int objectStart = responseText.indexOf('{');
        int objectEnd = responseText.lastIndexOf('}');
        if (objectStart < 0 || objectEnd <= objectStart) {
            throw new IllegalOperationException("The AI service returned an invalid structured response");
        }

        try {
            JsonNode result = objectMapper.readTree(responseText.substring(objectStart, objectEnd + 1));
            if (result == null || !result.isObject()) {
                throw new IllegalOperationException("The AI service returned an invalid structured response");
            }
            return result;
        } catch (JsonProcessingException exception) {
            throw new IllegalOperationException("The AI service returned malformed structured JSON");
        }
    }

    private List<String> readTextArray(JsonNode root, String property) {
        JsonNode values = root.get(property);
        if (values == null || !values.isArray()) {
            throw new IllegalOperationException("The AI service response is missing the '" + property + "' array");
        }

        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isTextual()) {
                throw new IllegalOperationException("The AI service returned a non-text " + property + " entry");
            }
            String text = value.asText().trim();
            if (!text.isBlank() && !result.contains(text)) {
                result.add(text);
            }
        }
        return result;
    }

    private String removeCodeFences(String content) {
        if (content.startsWith("```")) {
            int firstLineEnd = content.indexOf('\n');
            int lastFence = content.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return content.substring(firstLineEnd + 1, lastFence);
            }
        }
        return content;
    }

    private String stripTrailingSlashes(String value) {
        return value.replaceFirst("/+$", "");
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
