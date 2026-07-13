package com.sep.mmms_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    public void sendInviteEmail(String toEmail, String inviteToken, String inviterName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Invitation to join MeMin");
        message.setText("Hello,\n\n" + inviterName + " has invited you to join MeMin.\n\n" +
                "Please click the link below to register:\n" +
                "http://localhost:4200/register?token=" + inviteToken + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Regards,\nMeMin Team");
        
        try {
            emailSender.send(message);
        } catch (Exception e) {
            System.err.println("============= EMAIL SEND FAILED =============");
            System.err.println("To: " + toEmail);
            System.err.println("Invite link: http://localhost:4200/register?token=" + inviteToken);
            System.err.println("===========================================");
        }
    }
}
