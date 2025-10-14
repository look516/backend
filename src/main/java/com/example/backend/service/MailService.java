package com.example.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


@Service
@Slf4j
public class MailService {
    @Value("${spring.mail.username}")
    private String user;

    @Autowired
    private JavaMailSender javaMailSender;

    public String sendMail(String to, String subject, String text) {
        String test = "성공";
        try {
            MimeMessage m = javaMailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(m, "UTF-8");
            h.setFrom(user);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(text);
            javaMailSender.send(m);
        } catch (Exception e) {
            e.printStackTrace();
            test = "실패";
        }
        return test;
    }
}