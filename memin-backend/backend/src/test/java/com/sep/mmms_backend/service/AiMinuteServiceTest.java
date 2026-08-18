package com.sep.mmms_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.dto.AgendaDto;
import com.sep.mmms_backend.dto.DecisionDto;
import com.sep.mmms_backend.dto.MinuteDataDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiMinuteServiceTest {

    @Test
    void extractsOnlyStructuredItemsFromAnthropicCompatibleProvider() throws Exception {
        var restClientBuilder = org.springframework.web.client.RestClient.builder()
                .baseUrl("https://llm.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AiMinuteService service = new AiMinuteService(restClientBuilder, new ObjectMapper());
        ReflectionTestUtils.setField(service, "baseUrl", "https://llm.test");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");

        String providerText = "```json\n{\"agendas\":[\"Review the annual plan\"],\"decisions\":[\"Approve the annual plan\"]}\n```";
        String providerResponse = new ObjectMapper().writeValueAsString(
                java.util.Map.of("content", List.of(java.util.Map.of("type", "text", "text", providerText))));

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
}
