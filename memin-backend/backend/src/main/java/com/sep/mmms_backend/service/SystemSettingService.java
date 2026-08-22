package com.sep.mmms_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.mmms_backend.dto.AiSettingsDto;
import com.sep.mmms_backend.dto.EmailSettingsDto;
import com.sep.mmms_backend.dto.SystemSettingsDto;
import com.sep.mmms_backend.entity.SystemSetting;
import com.sep.mmms_backend.exceptions.IllegalOperationException;
import com.sep.mmms_backend.repository.SystemSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    // Fallback AI properties from environment / application.properties
    @Value("${llm.base-url:}")
    private String defaultAiBaseUrl;

    @Value("${llm.api-key:}")
    private String defaultAiApiKey;

    @Value("${llm.model:mimo-v2.5-pro}")
    private String defaultAiModel;

    @Value("${llm.max-tokens:2500}")
    private int defaultAiMaxTokens;

    // Fallback Email properties from environment / application.properties
    @Value("${spring.mail.host:localhost}")
    private String defaultMailHost;

    @Value("${spring.mail.port:1025}")
    private int defaultMailPort;

    @Value("${spring.mail.username:}")
    private String defaultMailUsername;

    @Value("${spring.mail.password:}")
    private String defaultMailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean defaultMailAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean defaultMailStarttls;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String defaultFrontendUrl;

    public SystemSettingService(
            SystemSettingRepository systemSettingRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.systemSettingRepository = systemSettingRepository;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SystemSettingsDto getSystemSettingsDto() {
        SystemSetting setting = systemSettingRepository.findDefaultSettings().orElse(null);

        AiSettingsDto effectiveAi = getEffectiveAiSettings();
        EmailSettingsDto effectiveEmail = getEffectiveEmailSettings();

        AiSettingsDto aiDto = AiSettingsDto.builder()
                .providerType(effectiveAi.getProviderType())
                .baseUrl(effectiveAi.getBaseUrl())
                .apiKey(null) // Mask secret key
                .hasApiKey(effectiveAi.getApiKey() != null && !effectiveAi.getApiKey().isBlank())
                .model(effectiveAi.getModel())
                .maxTokens(effectiveAi.getMaxTokens())
                .build();

        EmailSettingsDto emailDto = EmailSettingsDto.builder()
                .host(effectiveEmail.getHost())
                .port(effectiveEmail.getPort())
                .username(effectiveEmail.getUsername())
                .password(null) // Mask secret password
                .hasPassword(effectiveEmail.getPassword() != null && !effectiveEmail.getPassword().isBlank())
                .auth(effectiveEmail.getAuth())
                .starttls(effectiveEmail.getStarttls())
                .fromAddress(effectiveEmail.getFromAddress())
                .frontendUrl(effectiveEmail.getFrontendUrl())
                .build();

        return SystemSettingsDto.builder()
                .ai(aiDto)
                .email(emailDto)
                .updatedAt(setting != null ? setting.getUpdatedAt() : null)
                .updatedBy(setting != null ? setting.getUpdatedBy() : null)
                .build();
    }

    @Transactional
    public SystemSettingsDto updateSystemSettings(SystemSettingsDto updateDto, String updatedBy) {
        SystemSetting setting = systemSettingRepository.findDefaultSettings().orElseGet(() -> {
            SystemSetting s = new SystemSetting();
            s.setId(1);
            return s;
        });

        if (updateDto.getAi() != null) {
            AiSettingsDto ai = updateDto.getAi();
            if (ai.getProviderType() != null && !ai.getProviderType().isBlank()) {
                setting.setAiProviderType(ai.getProviderType().trim().toUpperCase());
            }
            if (ai.getBaseUrl() != null) {
                setting.setAiBaseUrl(ai.getBaseUrl().trim());
            }
            if (ai.getApiKey() != null && !ai.getApiKey().isBlank()) {
                setting.setAiApiKey(ai.getApiKey().trim());
            }
            if (ai.getModel() != null && !ai.getModel().isBlank()) {
                setting.setAiModel(ai.getModel().trim());
            }
            if (ai.getMaxTokens() != null && ai.getMaxTokens() > 0) {
                setting.setAiMaxTokens(ai.getMaxTokens());
            }
        }

        if (updateDto.getEmail() != null) {
            EmailSettingsDto email = updateDto.getEmail();
            if (email.getHost() != null) {
                setting.setMailHost(email.getHost().trim());
            }
            if (email.getPort() != null) {
                setting.setMailPort(email.getPort());
            }
            if (email.getUsername() != null) {
                setting.setMailUsername(email.getUsername().trim());
            }
            if (email.getPassword() != null && !email.getPassword().isBlank()) {
                setting.setMailPassword(email.getPassword().trim());
            }
            if (email.getAuth() != null) {
                setting.setMailAuth(email.getAuth());
            }
            if (email.getStarttls() != null) {
                setting.setMailStarttls(email.getStarttls());
            }
            if (email.getFromAddress() != null) {
                setting.setMailFrom(email.getFromAddress().trim());
            }
            if (email.getFrontendUrl() != null) {
                setting.setFrontendUrl(email.getFrontendUrl().trim());
            }
        }

        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(LocalDateTime.now());
        systemSettingRepository.save(setting);

        return getSystemSettingsDto();
    }

    @Transactional(readOnly = true)
    public AiSettingsDto getEffectiveAiSettings() {
        SystemSetting setting = systemSettingRepository.findDefaultSettings().orElse(null);

        String providerType = setting != null && setting.getAiProviderType() != null && !setting.getAiProviderType().isBlank()
                ? setting.getAiProviderType()
                : detectProviderType(setting != null ? setting.getAiBaseUrl() : defaultAiBaseUrl);

        String baseUrl = setting != null && setting.getAiBaseUrl() != null && !setting.getAiBaseUrl().isBlank()
                ? setting.getAiBaseUrl()
                : defaultAiBaseUrl;

        String apiKey = setting != null && setting.getAiApiKey() != null && !setting.getAiApiKey().isBlank()
                ? setting.getAiApiKey()
                : defaultAiApiKey;

        String model = setting != null && setting.getAiModel() != null && !setting.getAiModel().isBlank()
                ? setting.getAiModel()
                : defaultAiModel;

        int maxTokens = setting != null && setting.getAiMaxTokens() != null && setting.getAiMaxTokens() > 0
                ? setting.getAiMaxTokens()
                : defaultAiMaxTokens;

        return AiSettingsDto.builder()
                .providerType(providerType)
                .baseUrl(baseUrl != null ? baseUrl.trim() : "")
                .apiKey(apiKey != null ? apiKey.trim() : "")
                .hasApiKey(apiKey != null && !apiKey.isBlank())
                .model(model != null ? model.trim() : "")
                .maxTokens(maxTokens)
                .build();
    }

    @Transactional(readOnly = true)
    public EmailSettingsDto getEffectiveEmailSettings() {
        SystemSetting setting = systemSettingRepository.findDefaultSettings().orElse(null);

        String host = setting != null && setting.getMailHost() != null && !setting.getMailHost().isBlank()
                ? setting.getMailHost()
                : defaultMailHost;

        int port = setting != null && setting.getMailPort() != null && setting.getMailPort() > 0
                ? setting.getMailPort()
                : defaultMailPort;

        String username = setting != null && setting.getMailUsername() != null && !setting.getMailUsername().isBlank()
                ? setting.getMailUsername()
                : defaultMailUsername;

        String password = setting != null && setting.getMailPassword() != null && !setting.getMailPassword().isBlank()
                ? setting.getMailPassword()
                : defaultMailPassword;

        boolean auth = setting != null && setting.getMailAuth() != null
                ? setting.getMailAuth()
                : defaultMailAuth;

        boolean starttls = setting != null && setting.getMailStarttls() != null
                ? setting.getMailStarttls()
                : defaultMailStarttls;

        String from = setting != null && setting.getMailFrom() != null && !setting.getMailFrom().isBlank()
                ? setting.getMailFrom()
                : username;

        String frontendUrl = setting != null && setting.getFrontendUrl() != null && !setting.getFrontendUrl().isBlank()
                ? setting.getFrontendUrl()
                : defaultFrontendUrl;

        return EmailSettingsDto.builder()
                .host(host != null ? host.trim() : "")
                .port(port)
                .username(username != null ? username.trim() : "")
                .password(password != null ? password.trim() : "")
                .hasPassword(password != null && !password.isBlank())
                .auth(auth)
                .starttls(starttls)
                .fromAddress(from != null ? from.trim() : "")
                .frontendUrl(frontendUrl != null ? frontendUrl.trim() : "http://localhost:4200")
                .build();
    }

    public JavaMailSender createMailSender() {
        EmailSettingsDto settings = getEffectiveEmailSettings();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getHost());
        mailSender.setPort(settings.getPort() != null ? settings.getPort() : 587);

        if (settings.getUsername() != null && !settings.getUsername().isBlank()) {
            mailSender.setUsername(settings.getUsername());
        }
        if (settings.getPassword() != null && !settings.getPassword().isBlank()) {
            mailSender.setPassword(settings.getPassword());
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(settings.getAuth())));
        props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(settings.getStarttls())));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        return mailSender;
    }

    public boolean testEmail(String toEmail, String requestedBy) {
        EmailSettingsDto emailSettings = getEffectiveEmailSettings();
        if (emailSettings.getHost() == null || emailSettings.getHost().isBlank()) {
            throw new IllegalOperationException("SMTP host is not configured");
        }

        try {
            JavaMailSender mailSender = createMailSender();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);

            String from = emailSettings.getFromAddress();
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            } else if (emailSettings.getUsername() != null && !emailSettings.getUsername().isBlank()) {
                message.setFrom(emailSettings.getUsername());
            }

            message.setSubject("MeMin: SMTP Test Email");
            message.setText("Hello,\n\n" +
                    "This is a test email sent from MeMin system settings by " + requestedBy + ".\n" +
                    "Your email SMTP configuration is working correctly!\n\n" +
                    "Timestamp: " + LocalDateTime.now() + "\n\n" +
                    "Regards,\nMeMin Team");

            mailSender.send(message);
            log.info("Test email successfully sent to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send test email to {}: {}", toEmail, e.getMessage(), e);
            throw new IllegalOperationException("Failed to send test email: " + e.getMessage());
        }
    }

    public String testAiConnection(String customPrompt) {
        AiSettingsDto aiSettings = getEffectiveAiSettings();
        if (aiSettings.getBaseUrl() == null || aiSettings.getBaseUrl().isBlank()) {
            throw new IllegalOperationException("AI Base URL is not configured. Please set the Base URL.");
        }
        if (aiSettings.getApiKey() == null || aiSettings.getApiKey().isBlank()) {
            throw new IllegalOperationException("AI API Key is not configured. Please set the API Key.");
        }

        String prompt = (customPrompt != null && !customPrompt.isBlank())
                ? customPrompt
                : "Respond with a single short greeting confirming you are online and working.";

        boolean isOpenAi = "OPENAI_COMPATIBLE".equalsIgnoreCase(aiSettings.getProviderType());
        String baseUrl = cleanBaseUrl(aiSettings.getBaseUrl());

        try {
            if (isOpenAi) {
                String uri = baseUrl.endsWith("/v1") ? "/chat/completions" : "/v1/chat/completions";
                Map<String, Object> request = Map.of(
                        "model", aiSettings.getModel(),
                        "max_tokens", Math.min(200, aiSettings.getMaxTokens() != null ? aiSettings.getMaxTokens() : 200),
                        "messages", List.of(
                                Map.of("role", "system", "content", "You are a test assistant."),
                                Map.of("role", "user", "content", prompt)
                        )
                );

                Map<?, ?> response = restClientBuilder.baseUrl(baseUrl).build()
                        .post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + aiSettings.getApiKey())
                        .body(request)
                        .retrieve()
                        .body(Map.class);

                return extractOpenAiResponse(response);
            } else {
                // Anthropic
                String uri = baseUrl.endsWith("/v1") ? "/messages" : "/v1/messages";
                Map<String, Object> request = Map.of(
                        "model", aiSettings.getModel(),
                        "max_tokens", Math.min(200, aiSettings.getMaxTokens() != null ? aiSettings.getMaxTokens() : 200),
                        "messages", List.of(Map.of("role", "user", "content", prompt))
                );

                Map<?, ?> response = restClientBuilder.baseUrl(baseUrl).build()
                        .post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-api-key", aiSettings.getApiKey())
                        .header("api-key", aiSettings.getApiKey())
                        .header("anthropic-version", "2023-06-01")
                        .body(request)
                        .retrieve()
                        .body(Map.class);

                return extractAnthropicResponse(response);
            }
        } catch (RestClientResponseException exception) {
            throw new IllegalOperationException("The AI service rejected the test request (HTTP "
                    + exception.getStatusCode().value() + "): " + exception.getResponseBodyAsString());
        } catch (RestClientException exception) {
            throw new IllegalOperationException("The AI service could not be reached: " + exception.getMessage());
        }
    }

    private String extractAnthropicResponse(Map<?, ?> response) {
        JsonNode responseNode = objectMapper.valueToTree(response);
        JsonNode content = responseNode == null ? null : responseNode.path("content");
        if (content != null && content.isArray()) {
            for (JsonNode contentBlock : content) {
                String text = contentBlock.path("text").asText("");
                if (!text.isBlank()) {
                    return text.trim();
                }
            }
        }
        if (responseNode != null && responseNode.path("choices").isArray()) {
            return responseNode.path("choices").path(0).path("message").path("content").asText("").trim();
        }
        return "Connected successfully (Empty response text)";
    }

    private String extractOpenAiResponse(Map<?, ?> response) {
        JsonNode responseNode = objectMapper.valueToTree(response);
        if (responseNode != null && responseNode.path("choices").isArray()) {
            String text = responseNode.path("choices").path(0).path("message").path("content").asText("");
            if (!text.isBlank()) {
                return text.trim();
            }
        }
        if (responseNode != null && responseNode.path("content").isArray()) {
            return extractAnthropicResponse(response);
        }
        return "Connected successfully (Empty response text)";
    }

    private String detectProviderType(String baseUrl) {
        if (baseUrl != null) {
            String lower = baseUrl.toLowerCase();
            if (lower.contains("openai") || lower.contains("groq") || lower.contains("ollama")
                    || lower.contains("deepseek") || lower.contains("openrouter") || lower.contains("together")
                    || lower.contains("localhost") || lower.contains("127.0.0.1")) {
                return "OPENAI_COMPATIBLE";
            }
        }
        return "ANTHROPIC";
    }

    private String cleanBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        String cleaned = baseUrl.trim();
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }
}
