package com.madhurgram.productservice.cart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TwilioService {

    private static final Logger log = LoggerFactory.getLogger(TwilioService.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final HttpClient httpClient;

    public TwilioService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendWhatsAppMessage(String toPhoneNumber, String messageText) {
        // Normalize target phone number
        String cleanPhone = toPhoneNumber.replaceAll("\\D", "");
        String formattedTo = cleanPhone.length() == 10 ? "+91" + cleanPhone : "+" + cleanPhone;
        
        String twilioTo = "whatsapp:" + formattedTo;
        String twilioFrom = "whatsapp:" + fromNumber;

        // Check if Twilio configuration is in Mock Mode
        if (accountSid == null || accountSid.trim().isEmpty() || accountSid.startsWith("ACmock")) {
            log.info("[MOCK TWILIO WHATSAPP DISPATCH] Successfully triggered in DEV mode.");
            log.info("From: {}", twilioFrom);
            log.info("To: {}", twilioTo);
            log.info("Message Body:\n{}", messageText);
            return;
        }

        try {
            log.info("Sending live WhatsApp message via Twilio API to: {}", twilioTo);
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            
            Map<String, String> formData = Map.of(
                "To", twilioTo,
                "From", twilioFrom,
                "Body", messageText
            );

            String requestBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + 
                          URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Twilio WhatsApp reminder dispatched successfully! Response Status: {}", response.statusCode());
            } else {
                log.error("Twilio API rejected the request. Status Code: {}, Response: {}", response.statusCode(), response.body());
                throw new RuntimeException("Twilio API Error status " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message via Twilio API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to dispatch WhatsApp message via Twilio API: " + e.getMessage());
        }
    }
}
