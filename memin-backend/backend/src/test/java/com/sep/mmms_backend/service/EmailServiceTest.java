package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.EmailSettingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SystemSettingService systemSettingService;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, systemSettingService);
    }

    @Test
    void sendInviteEmailUsesCurrentSmtpSettingsAndAllowsExternalDomains() {
        when(systemSettingService.getEffectiveEmailSettings()).thenReturn(EmailSettingsDto.builder()
                .username("department@example.com")
                .fromAddress("MeMin <department@example.com>")
                .frontendUrl("https://memin.example.com/")
                .build());
        when(systemSettingService.createMailSender()).thenReturn(mailSender);

        boolean sent = emailService.sendInviteEmail(
                "recipient@external-organization.org",
                "invite-token",
                "Department Head");

        assertThat(sent).isTrue();
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("recipient@external-organization.org");
        assertThat(message.getFrom()).isEqualTo("MeMin <department@example.com>");
        assertThat(message.getText()).contains("https://memin.example.com/register?token=invite-token");
    }

    @Test
    void sendInviteEmailReturnsFalseWhenCurrentSmtpSenderFails() {
        when(systemSettingService.getEffectiveEmailSettings()).thenReturn(EmailSettingsDto.builder()
                .username("department@example.com")
                .frontendUrl("https://memin.example.com")
                .build());
        when(systemSettingService.createMailSender()).thenReturn(mailSender);
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP unavailable"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        boolean sent = emailService.sendInviteEmail(
                "recipient@external-organization.org", "invite-token", "Department Head");

        assertThat(sent).isFalse();
    }
}
