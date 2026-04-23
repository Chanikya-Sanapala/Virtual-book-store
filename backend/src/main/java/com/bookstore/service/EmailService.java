package com.bookstore.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to LeafyBooks! 📚");
            
            String htmlMsg = "<div style='font-family: Arial, sans-serif; color: #2d3436; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e8ed; border-radius: 10px;'>"
                           + "<h2 style='color: #00b894; text-align: center;'>Welcome to LeafyBooks!</h2>"
                           + "<p style='font-size: 16px;'>Hi <b>" + username + "</b>,</p>"
                           + "<p style='font-size: 16px; line-height: 1.5;'>We are absolutely thrilled to have you join our community! LeafyBooks is your new home for discovering amazing stories, sharing your favorites, and building your dream library.</p>"
                           + "<div style='background-color: #f8fbfd; padding: 15px; border-left: 4px solid #00b894; margin: 20px 0;'>"
                           + "<h3 style='margin-top: 0; color: #2d3436;'>Here is what you can do next:</h3>"
                           + "<ul style='font-size: 15px; line-height: 1.6;'>"
                           + "<li>Browse our curated catalog of books.</li>"
                           + "<li>Add items to your cart and check out securely.</li>"
                           + "<li>Join community discussions and leave your thoughts.</li>"
                           + "</ul>"
                           + "</div>"
                           + "<p style='font-size: 16px;'>If you ever need any help, just reply to this email.</p>"
                           + "<br>"
                           + "<p style='font-size: 16px;'>Happy Reading,<br><b>The LeafyBooks Team</b></p>"
                           + "</div>";
            
            helper.setText(htmlMsg, true); // true indicates HTML
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send HTML welcome email: " + e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("LeafyBooks - Password Reset Request");
        message.setText("You have requested to reset your password.\n\nPlease click the link below to set a new password:\n" + resetLink + "\n\nIf you did not request this, please ignore this email.\n\nThe LeafyBooks Team");
        mailSender.send(message);
    }
}
