package com.sep.mmms_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AiMinuteServiceTest {

    @Mock
    private SystemSettingService systemSettingService;

    @Test
    void extractsOnlyStructuredItemsFromAnthropicCompatibleProvider() throws Exception {
        var restClientBuilder = RestClient.builder().baseUrl("https://llm.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        AiMinuteService service = new AiMinuteService(restClientBuilder, new ObjectMapper(), systemSettingService);

        when(systemSettingService.getEffectiveAiSettings()).thenReturn(AiSettingsDto.builder()
                .providerType("ANTHROPIC")
                .baseUrl("https://llm.test")
                .apiKey("test-key")
                .model("test-model")
                .maxTokens(2500)
                .build());

        String providerText = "```json\n{\"agendas\":[\"Review the annual plan\"],\"decisions\":[\"Approve the annual plan\"]}\n```";
        String providerResponse = new ObjectMapper().writeValueAsString(
                Map.of("content", List.of(Map.of("type", "text", "text", providerText))));

        server.expect(requestTo("https://llm.test/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-key"))
                .andExpect(content().string(containsString("Return only one valid JSON object")))
                .andExpect(content().string(containsString("Existing agenda entries")))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        MinuteDataDto input = new MinuteDataDto();
        input.setMeetingTitle("Annual review");
        input.setAgendas(List.of(new AgendaDto(1, "annual plan")));
        input.setDecisions(List.of(new DecisionDto(2, "approve plan")));

        var result = service.extractStructuredItems(input, "Use formal wording");

        assertThat(result.getAgendas()).extracting(AgendaDto::getAgenda)
                .containsExactly("Review the annual plan");
        assertThat(result.getDecisions()).extracting(DecisionDto::getDecision)
                .containsExactly("Approve the annual plan");
        server.verify();
    }

    @Test
    void extractsStructuredItemsFromOpenAiCompatibleProvider() throws Exception {
        var restClientBuilder = RestClient.builder().baseUrl("https://openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        AiMinuteService service = new AiMinuteService(restClientBuilder, new ObjectMapper(), systemSettingService);

        when(systemSettingService.getEffectiveAiSettings()).thenReturn(AiSettingsDto.builder()
                .providerType("OPENAI_COMPATIBLE")
                .baseUrl("https://openai.test")
                .apiKey("sk-openai-key")
                .model("gpt-4o-mini")
                .maxTokens(2500)
                .build());

        String providerText = "{\"agendas\":[\"Budget allocation\"],\"decisions\":[\"Allocated 100k\"]}";
        String providerResponse = new ObjectMapper().writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("content", providerText)))));

        server.expect(requestTo("https://openai.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-openai-key"))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        MinuteDataDto input = new MinuteDataDto();
        input.setMeetingTitle("Budget review");
        input.setAgendas(List.of(new AgendaDto(1, "budget")));
        input.setDecisions(List.of(new DecisionDto(2, "100k approved")));

        var result = service.extractStructuredItems(input, "Formal phrasing");

        assertThat(result.getAgendas()).extracting(AgendaDto::getAgenda)
                .containsExactly("Budget allocation");
        assertThat(result.getDecisions()).extracting(DecisionDto::getDecision)
                .containsExactly("Allocated 100k");
        server.verify();
    }

    @Test
    void extractsStructuredItemsFromOpenAiResponsesProvider() throws Exception {
        var restClientBuilder = RestClient.builder().baseUrl("https://opencode.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        AiMinuteService service = new AiMinuteService(restClientBuilder, new ObjectMapper(), systemSettingService);

        when(systemSettingService.getEffectiveAiSettings()).thenReturn(AiSettingsDto.builder()
                .providerType("OPENAI_RESPONSES")
                .baseUrl("https://opencode.test/v1")
                .apiKey("sk-opencode-key")
                .model("muse-spark-1.2-contributor-free")
                .maxTokens(1500)
                .build());

        String providerText = "{\"agendas\":[\"Budget allocation\"],\"decisions\":[\"Allocated 100k\"]}";
        String providerResponse = new ObjectMapper().writeValueAsString(
                Map.of("output", List.of(Map.of(
                        "type", "message",
                        "content", List.of(Map.of("type", "output_text", "text", providerText))))));

        server.expect(requestTo("https://opencode.test/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-opencode-key"))
                .andExpect(content().string(containsString("\"input\"")))
                .andExpect(content().string(containsString("\"max_output_tokens\"")))
                .andExpect(content().string(containsString("\"safety_identifier\":\"memin-application\"")))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        MinuteDataDto input = new MinuteDataDto();
        input.setMeetingTitle("Budget review");
        input.setAgendas(List.of(new AgendaDto(1, "budget")));
        input.setDecisions(List.of(new DecisionDto(2, "100k approved")));

        var result = service.extractStructuredItems(input, "Formal phrasing");

        assertThat(result.getAgendas()).extracting(AgendaDto::getAgenda)
                .containsExactly("Budget allocation");
        assertThat(result.getDecisions()).extracting(DecisionDto::getDecision)
                .containsExactly("Allocated 100k");
        server.verify();
    }
}
