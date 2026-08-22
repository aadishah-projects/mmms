package com.sep.mmms_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.dto.AiSettingsDto;
import com.sep.mmms_backend.dto.EmailSettingsDto;
import com.sep.mmms_backend.dto.SystemSettingsDto;
import com.sep.mmms_backend.entity.SystemSetting;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.repository.SystemSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
public class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    private RestClient.Builder restClientBuilder;
    private ObjectMapper objectMapper;
    private SystemSettingService systemSettingService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        objectMapper = new ObjectMapper();
        systemSettingService = new SystemSettingService(
                systemSettingRepository,
                restClientBuilder,
                objectMapper
        );

        // Inject default fallbacks
        ReflectionTestUtils.setField(systemSettingService, "defaultAiBaseUrl", "https://api.anthropic.com");
        ReflectionTestUtils.setField(systemSettingService, "defaultAiApiKey", "default-key");
        ReflectionTestUtils.setField(systemSettingService, "defaultAiModel", "claude-3-haiku");
        ReflectionTestUtils.setField(systemSettingService, "defaultAiMaxTokens", 2000);
        ReflectionTestUtils.setField(systemSettingService, "defaultMailHost", "smtp.default.com");
        ReflectionTestUtils.setField(systemSettingService, "defaultMailPort", 587);
        ReflectionTestUtils.setField(systemSettingService, "defaultMailUsername", "default@test.com");
        ReflectionTestUtils.setField(systemSettingService, "defaultMailPassword", "default-pass");
        ReflectionTestUtils.setField(systemSettingService, "defaultFrontendUrl", "http://localhost:4200");
    }

    @Test
    void getSystemSettingsDto_WhenDbIsEmpty_ReturnsDefaultsWithMaskedSecrets() {
        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.empty());

        SystemSettingsDto result = systemSettingService.getSystemSettingsDto();

        assertThat(result).isNotNull();
        assertThat(result.getAi().getBaseUrl()).isEqualTo("https://api.anthropic.com");
        assertThat(result.getAi().getApiKey()).isNull(); // Secret masked
        assertThat(result.getAi().isHasApiKey()).isTrue();
        assertThat(result.getAi().getModel()).isEqualTo("claude-3-haiku");

        assertThat(result.getEmail().getHost()).isEqualTo("smtp.default.com");
        assertThat(result.getEmail().getPassword()).isNull(); // Secret masked
        assertThat(result.getEmail().isHasPassword()).isTrue();
    }

    @Test
    void updateSystemSettings_SavesToDbAndRetainsExistingKeyIfBlank() {
        SystemSetting existing = SystemSetting.builder()
                .id(1)
                .aiApiKey("previous-secret-key")
                .mailPassword("previous-mail-pass")
                .build();

        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.of(existing));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemSettingsDto updateRequest = SystemSettingsDto.builder()
                .ai(AiSettingsDto.builder()
                        .providerType("OPENAI_COMPATIBLE")
                        .baseUrl("https://api.openai.com")
                        .apiKey("") // blank, should retain previous
                        .model("gpt-4o-mini")
                        .maxTokens(1500)
                        .build())
                .email(EmailSettingsDto.builder()
                        .host("smtp.gmail.com")
                        .port(465)
                        .username("admin@gmail.com")
                        .password("") // blank, should retain previous
                        .auth(true)
                        .starttls(true)
                        .fromAddress("MeMin <admin@gmail.com>")
                        .frontendUrl("https://memin.example.com")
                        .build())
                .build();

        SystemSettingsDto result = systemSettingService.updateSystemSettings(updateRequest, "deptHead");

        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository).save(captor.capture());

        SystemSetting saved = captor.getValue();
        assertThat(saved.getAiProviderType()).isEqualTo("OPENAI_COMPATIBLE");
        assertThat(saved.getAiBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(saved.getAiApiKey()).isEqualTo("previous-secret-key"); // Retained!
        assertThat(saved.getMailHost()).isEqualTo("smtp.gmail.com");
        assertThat(saved.getMailPassword()).isEqualTo("previous-mail-pass"); // Retained!
        assertThat(saved.getUpdatedBy()).isEqualTo("deptHead");

        assertThat(result.getAi().isHasApiKey()).isTrue();
        assertThat(result.getEmail().isHasPassword()).isTrue();
    }

    @Test
    void createMailSender_ConfiguresJavaMailSenderPropertiesCorrectly() {
        SystemSetting setting = SystemSetting.builder()
                .id(1)
                .mailHost("smtp.sendgrid.net")
                .mailPort(587)
                .mailUsername("apikey")
                .mailPassword("SG.secret")
                .mailAuth(true)
                .mailStarttls(true)
                .build();

        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.of(setting));

        JavaMailSenderImpl mailSender = (JavaMailSenderImpl) systemSettingService.createMailSender();

        assertThat(mailSender.getHost()).isEqualTo("smtp.sendgrid.net");
        assertThat(mailSender.getPort()).isEqualTo(587);
        assertThat(mailSender.getUsername()).isEqualTo("apikey");
        assertThat(mailSender.getPassword()).isEqualTo("SG.secret");
        assertThat(mailSender.getJavaMailProperties().getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(mailSender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
    }

    @Test
    void createMailSender_RereadsUpdatedDatabaseSettingsWithoutRestart() {
        AtomicReference<SystemSetting> current = new AtomicReference<>(SystemSetting.builder()
                .id(1)
                .mailHost("smtp.first.example")
                .mailPort(587)
                .mailUsername("first@example.com")
                .mailPassword("first-secret")
                .mailAuth(true)
                .mailStarttls(true)
                .build());
        when(systemSettingRepository.findDefaultSettings())
                .thenAnswer(invocation -> Optional.of(current.get()));

        JavaMailSenderImpl firstSender = (JavaMailSenderImpl) systemSettingService.createMailSender();

        current.set(SystemSetting.builder()
                .id(1)
                .mailHost("smtp.second.example")
                .mailPort(465)
                .mailUsername("second@example.com")
                .mailPassword("second-secret")
                .mailAuth(true)
                .mailStarttls(false)
                .build());
        JavaMailSenderImpl secondSender = (JavaMailSenderImpl) systemSettingService.createMailSender();

        assertThat(firstSender.getHost()).isEqualTo("smtp.first.example");
        assertThat(secondSender.getHost()).isEqualTo("smtp.second.example");
        assertThat(secondSender.getPort()).isEqualTo(465);
        assertThat(secondSender.getUsername()).isEqualTo("second@example.com");
        assertThat(secondSender.getPassword()).isEqualTo("second-secret");
        assertThat(secondSender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"))
                .isEqualTo("false");
    }

    @Test
    void testAiConnection_WithOpenAiCompatibleProvider_Succeeds() {
        SystemSetting setting = SystemSetting.builder()
                .id(1)
                .aiProviderType("OPENAI_COMPATIBLE")
                .aiBaseUrl("https://api.openai.test")
                .aiApiKey("sk-test-key")
                .aiModel("gpt-4o-mini")
                .aiMaxTokens(500)
                .build();

        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.of(setting));

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"Hello from OpenAI\"}}]}", MediaType.APPLICATION_JSON));

        String response = systemSettingService.testAiConnection("Ping");

        assertThat(response).isEqualTo("Hello from OpenAI");
        server.verify();
    }

    @Test
    void getEffectiveAiSettings_NormalizesLegacyOpenCodeMuseConfigurationToResponses() {
        SystemSetting setting = SystemSetting.builder()
                .id(1)
                .aiProviderType("ANTHROPIC")
                .aiBaseUrl("https://opencode.ai/zen/v1")
                .aiApiKey("sk-test-key")
                .aiModel("muse-spark-1.2-contributor-free")
                .aiMaxTokens(1500)
                .build();

        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.of(setting));

        AiSettingsDto result = systemSettingService.getEffectiveAiSettings();

        assertThat(result.getProviderType()).isEqualTo("OPENAI_RESPONSES");
    }

    @Test
    void testAiConnection_WithOpenAiResponsesProvider_Succeeds() {
        SystemSetting setting = SystemSetting.builder()
                .id(1)
                .aiProviderType("OPENAI_RESPONSES")
                .aiBaseUrl("https://opencode.ai/zen/v1")
                .aiApiKey("sk-test-key")
                .aiModel("muse-spark-1.2-contributor-free")
                .aiMaxTokens(1500)
                .build();

        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.of(setting));

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://opencode.ai/zen/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andExpect(content().string(containsString("\"input\"")))
                .andExpect(content().string(containsString("\"max_output_tokens\"")))
                .andExpect(content().string(containsString("\"safety_identifier\":\"memin-settings-test\"")))
                .andRespond(withSuccess(
                        "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"Hello from Responses\"}]}]}",
                        MediaType.APPLICATION_JSON));

        String response = systemSettingService.testAiConnection("Ping");

        assertThat(response).isEqualTo("Hello from Responses");
        server.verify();
    }

    @Test
    void testAiConnection_WithAnthropicProvider_Succeeds() {
        SystemSetting setting = SystemSetting.builder()
                .id(1)
                .aiProviderType("ANTHROPIC")
                .aiBaseUrl("https://api.anthropic.test")
                .aiApiKey("sk-ant-test-key")
                .aiModel("claude-3-haiku")
                .aiMaxTokens(500)
                .build();

        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.of(setting));

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://api.anthropic.test/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "sk-ant-test-key"))
                .andRespond(withSuccess("{\"content\":[{\"type\":\"text\",\"text\":\"Hello from Claude\"}]}", MediaType.APPLICATION_JSON));

        String response = systemSettingService.testAiConnection("Ping");

        assertThat(response).isEqualTo("Hello from Claude");
        server.verify();
    }

    @Test
    void testAiConnection_WhenNotConfigured_ThrowsException() {
        ReflectionTestUtils.setField(systemSettingService, "defaultAiBaseUrl", "");
        when(systemSettingRepository.findDefaultSettings()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemSettingService.testAiConnection("test"))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("AI Base URL is not configured");
    }
}
