package com.ecommerce.user_service.services;

import com.ecommerce.user_service.entities.TokenPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@ecommerce.local}")
    private String from;

    @Value("${app.security.password-reset-token-ttl-minutes:15}")
    private long tokenTtlMinutes;

    public void sendTokenEmail(String to, TokenPurpose purpose, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);

        if (purpose == TokenPurpose.EMAIL_VERIFICATION) {
            message.setSubject("Verify your email");
            message.setText(
                    "Use this code to verify your email:\n\n" +
                    token + "\n\n" +
                    "It expires in " + tokenTtlMinutes + " minutes."
            );
        } else {
            message.setSubject("Reset your password");
            message.setText(
                    "Use this code to reset your password:\n\n" +
                    token + "\n\n" +
                    "It expires in " + tokenTtlMinutes + " minutes."
            );
        }

        mailSender.send(message);
    }
}
