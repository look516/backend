package com.example.backend.controller;

import com.example.backend.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;

@RestController
@Slf4j
@RequestMapping("/api/mail")
public class MailController {
    private MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @RequestMapping("/test")
    public String MailController(HttpServletRequest req) throws ParseException, IOException {
        String result;
        result = mailService.sendMail(String.valueOf(req.getRequestURL()), "제목", "내용");
        return result;
    }
}
