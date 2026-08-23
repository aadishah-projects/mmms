package com.sep.mmms_backend.service;

import com.sep.mmms_backend.dto.EmailSettingsDto;
import com.sep.mmms_backend.entity.Meeting;
import org.springframework.core.io.ByteArrayResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import jakarta.mail.MessagingException;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender emailSender;

    @Autowired(required = false)
    private SystemSettingService systemSettingService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public EmailService() {}

    public EmailService(JavaMailSender emailSender, SystemSettingService systemSettingService) {
        this.emailSender = emailSender;
        this.systemSettingService = systemSettingService;
    }

    public boolean sendInviteEmail(String toEmail, String inviteToken, String inviterName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        setFromIfConfigured(message);
        message.setSubject("Invitation to join MeMin");
        message.setText("Hello,\n\n" + inviterName + " has invited you to join MeMin.\n\n" +
                "Please click the link below to register:\n" +
                buildFrontendUrl("/register?token=" + URLEncoder.encode(inviteToken, StandardCharsets.UTF_8)) + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Regards,\nMeMin Team");

        return send(message, "registration invitation");
    }

    public boolean sendMeetingMinutesEmail(String toEmail, Meeting meeting, String senderName, byte[] attachment) {
        try {
            JavaMailSender sender = getActiveMailSender();
            if (sender == null) {
                log.warn("No mail sender available to send meeting minutes. Check SMTP settings.");
                return false;
            }

            var message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());
            helper.setTo(toEmail);
            setFromIfConfigured(helper);
            helper.setSubject("Meeting minutes available: " + meeting.getTitle());

            String heldDate = meeting.getHeldDate() == null ? "Not specified" : meeting.getHeldDate().toString();
            String heldTime = meeting.getHeldTime() == null ? "Not specified" : meeting.getHeldTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            String committeeName = meeting.getCommittee() == null ? "MeMin" : meeting.getCommittee().getName();
            String minuteUrl = buildFrontendUrl("/committee-details/overview/minute?committeeId="
                    + meeting.getCommittee().getId() + "&meetingId=" + meeting.getId());

            helper.setText("Hello,\n\n" +
                    "The final minute for today's meeting in " + committeeName + " is now available.\n\n" +
                    "Meeting: " + meeting.getTitle() + "\n" +
                    "Date: " + heldDate + "\n" +
                    "Time: " + heldTime + "\n" +
                    "Place: " + meeting.getHeldPlace() + "\n\n" +
                    "View the minute in MeMin: " + minuteUrl + "\n\n" +
                    "The final minute is attached to this email.\n\n" +
                    "Regards,\n" + senderName + "\nMeMin Team");
            helper.addAttachment(
                    buildMinuteAttachmentName(meeting),
                    new ByteArrayResource(attachment));

            sender.send(message);
            return true;
        } catch (MessagingException | RuntimeException e) {
            log.warn("Could not send meeting minutes to {}. The application record was still saved. Reason: {}",
                    toEmail, e.getMessage(), e);
            return false;
        }
    }

    private String buildMinuteAttachmentName(Meeting meeting) {
        String title = meeting.getTitle() == null ? "meeting" : meeting.getTitle();
        String safeTitle = title.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return "Meeting_Minutes_" + safeTitle + ".docx";
    }

    /*
     * Kept as a small reusable helper for the plain-text emails above. Meeting
     * creation now sends the final minute email instead of a scheduling invite.
     */
    private void setFromIfConfigured(SimpleMailMessage message) {
        String from = configuredFromAddress();
        if (from != null) {
            message.setFrom(from);
        }
    }

    private void setFromIfConfigured(MimeMessageHelper message) throws MessagingException {
        String from = configuredFromAddress();
        if (from != null) {
            message.setFrom(from);
        }
    }

    private String configuredFromAddress() {
        if (systemSettingService != null) {
            EmailSettingsDto emailSettings = systemSettingService.getEffectiveEmailSettings();
            String from = emailSettings.getFromAddress();
            if (from != null && !from.isBlank()) {
                return from;
            }
            if (emailSettings.getUsername() != null && !emailSettings.getUsername().isBlank()) {
                return emailSettings.getUsername();
            }
        }
        if (mailFrom != null && !mailFrom.isBlank()) {
            return mailFrom;
        }
        return null;
    }

    private boolean send(SimpleMailMessage message, String messageType) {
        try {
            JavaMailSender sender = getActiveMailSender();
            if (sender == null) {
                log.warn("No mail sender available to send {}. Check SMTP settings.", messageType);
                return false;
            }
            sender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Could not send {} to {}. The application record was still saved. Reason: {}",
                    messageType, String.join(",", message.getTo()), e.getMessage(), e);
            return false;
        }
    }

    private JavaMailSender getActiveMailSender() {
        if (systemSettingService != null) {
            return systemSettingService.createMailSender();
        }
        return emailSender;
    }

    private String buildFrontendUrl(String path) {
        String baseUrl = null;
        if (systemSettingService != null) {
            EmailSettingsDto emailSettings = systemSettingService.getEffectiveEmailSettings();
            baseUrl = emailSettings.getFrontendUrl();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }
}
