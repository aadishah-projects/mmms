package com.sep.mmms_backend.service;

import com.sep.mmms_backend.entity.Meeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

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

    public boolean sendMeetingInviteEmail(String toEmail, Meeting meeting, String inviterName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        setFromIfConfigured(message);
        message.setSubject("Meeting invitation: " + meeting.getTitle());
        String heldDate = meeting.getHeldDate() == null ? "Not specified" : meeting.getHeldDate().toString();
        String heldTime = meeting.getHeldTime() == null ? "Not specified" : meeting.getHeldTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String committeeName = meeting.getCommittee() == null ? "MeMin" : meeting.getCommittee().getName();
        message.setText("Hello,\n\n" + inviterName + " invited you to a meeting in " + committeeName + ".\n\n" +
                "Meeting: " + meeting.getTitle() + "\n" +
                "Date: " + heldDate + "\n" +
                "Time: " + heldTime + "\n" +
                "Place: " + meeting.getHeldPlace() + "\n\n" +
                "Sign in to MeMin to view the meeting:\n" +
                buildFrontendUrl("/login") + "\n\n" +
                "Regards,\nMeMin Team");

        return send(message, "meeting invitation");
    }

    private boolean send(SimpleMailMessage message, String messageType) {
        try {
            emailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Could not send {} to {}. The application record was still saved.", messageType, String.join(",", message.getTo()), e);
            return false;
        }
    }

    private void setFromIfConfigured(SimpleMailMessage message) {
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
    }

    private String buildFrontendUrl(String path) {
        String baseUrl = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }
}
