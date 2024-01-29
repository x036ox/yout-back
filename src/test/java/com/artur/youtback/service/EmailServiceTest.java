package com.artur.youtback.service;

import com.artur.youtback.YoutBackApplicationTests;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest extends YoutBackApplicationTests {
    
    @MockBean
    JavaMailSender javaMailSender;
    @Autowired
    EmailService emailService;

    @Test
    void sendConfirmationEmail() {
        emailService.sendConfirmationEmail("example@gmail.com");
        Mockito.verify(javaMailSender, Mockito.times(1)).send(Mockito.any(SimpleMailMessage.class));
    }
}