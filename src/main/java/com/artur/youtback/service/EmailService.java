package com.artur.youtback.service;

import com.artur.youtback.utils.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class EmailService {
    @Autowired
    JavaMailSender javaMailSender;

    public void sendConfirmationEmail(String email){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("yout.back.test@gmail.com");
        message.setTo(email);
        message.setSubject("Email confirmation yout");
        String encodedEmail = Base64.getEncoder().encodeToString(email.getBytes());

        StringBuilder text = new StringBuilder();
        text.append("To confirm your email please click one the link below:\n\n")
                        .append("Confirmation link: ")
                                .append(AppConstants.CLIENT_CONFIRMATION_LINK)
                                        .append(encodedEmail);

        message.setText(text.toString());
        javaMailSender.send(message);
    }
}
