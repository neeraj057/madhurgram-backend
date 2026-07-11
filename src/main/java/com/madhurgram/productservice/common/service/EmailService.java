package com.madhurgram.productservice.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendEmail(String to, String subject, String body) {
        log.info("\n" +
                "========================================================================\n" +
                "📧 [EMAIL TRANSMISSION SERVICE] (SIMULATED OUTGOING SMTP)\n" +
                "------------------------------------------------------------------------\n" +
                "To      : {}\n" +
                "Subject : {}\n" +
                "Message :\n" +
                "{}\n" +
                "========================================================================\n", to, subject, body);
    }
}
