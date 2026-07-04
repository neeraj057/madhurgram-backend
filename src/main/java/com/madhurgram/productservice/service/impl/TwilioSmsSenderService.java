package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.service.SmsSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class TwilioSmsSenderService implements SmsSenderService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsSenderService.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final HttpClient httpClient;

    public TwilioSmsSenderService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.httpClient = HttpClient.newHttpClient();

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank() || fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio configuration is missing. Configure twilio.account-sid, twilio.auth-token, and twilio.from-number.");
        }
    }

    @Override
    public int sendBroadcastMessage(List<String> recipients, String message) {
        if (recipients.isEmpty()) {
            return 0;
        }

        String endpoint = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid);
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        int successCount = 0;
        for (String recipient : recipients) {
            try {
                if (recipient == null || recipient.isBlank()) {
                    logger.warn("Skipping blank recipient phone number for broadcast message.");
                    continue;
                }

                String body = buildForm("To", recipient)
                        + "&" + buildForm("From", fromNumber)
                        + "&" + buildForm("Body", message);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", authorizationHeader)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    successCount++;
                } else {
                    logger.warn("Twilio send failed for {}. status={} response={}", recipient, response.statusCode(), response.body());
                }
            } catch (Exception e) {
                logger.error("Failed to send Twilio SMS to recipient {}", recipient, e);
            }
        }

        logger.info("Broadcast message sent to {} recipients out of {} attempted.", successCount, recipients.size());
        return successCount;
    }

    private String buildForm(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
