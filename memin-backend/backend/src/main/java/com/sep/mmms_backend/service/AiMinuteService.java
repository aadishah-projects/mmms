package com.sep.mmms_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.dto.MinuteDataDto;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class AiMinuteService {
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${llm.base-url:}")
    private String baseUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:mimo-v2.5-pro}")
    private String model;

    @Value("${llm.max-tokens:8000}")
    private int maxTokens;

    public AiMinuteService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public String generateMinute(MinuteDataDto minuteData, String roughPrompt) {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalOperationException("AI minute drafting is not configured. Set LLM_BASE_URL and LLM_API_KEY.");
        }

        String prompt = "Meeting details:\n" +
                "Committee: " + minuteData.getCommitteeName() + "\n" +
                "Description: " + minuteData.getCommitteeDescription() + "\n" +
                "Date: " + minuteData.getMeetingHeldDate() + "\n" +
                "Time: " + minuteData.getMeetingHeldTime() + "\n" +
                "Place: " + minuteData.getMeetingHeldPlace() + "\n" +
                "Coordinator: " + minuteData.getCoordinatorFullName() + "\n" +
                "Participants: " + minuteData.getParticipants().stream()
                .map(participant -> participant.getFullName() + " (" + participant.getRole() + ")")
                .toList() + "\n" +
                "Agendas: " + minuteData.getAgendas().stream().map(item -> item.getAgenda()).toList() + "\n" +
                "Existing decisions: " + minuteData.getDecisions().stream().map(item -> item.getDecision()).toList() + "\n\n" +
                "Additional rough instructions from the minute writer:\n" + roughPrompt.trim();

        String systemPrompt = "You are a formal meeting-minute writer. Draft a complete, professional minute " +
                "from the meeting facts and rough instructions. Return only an HTML fragment, without Markdown " +
                "code fences and without an outer #a4-box element. Include a clear opening paragraph, an " +
                "attendance table with a signature column, an agenda section when agendas exist, and a numbered " +
                "decisions section. Do not invent names, dates, votes, or decisions; use only the supplied facts " +
                "and clearly phrase any rough instruction as a proposed draft.";

        Map<String, Object> request = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            Map<?, ?> response = restClientBuilder.baseUrl(baseUrl).build()
                    .post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            JsonNode content = objectMapper.valueToTree(response).path("content");
            String generated = content.isArray() && !content.isEmpty()
                    ? content.get(0).path("text").asText("")
                    : "";
            generated = removeCodeFences(generated).trim();
            if (generated.isBlank()) {
                throw new IllegalOperationException("The AI service returned an empty minute draft");
            }
            return generated;
        } catch (RestClientException exception) {
            throw new IllegalOperationException("The AI minute service could not be reached");
        }
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
}
