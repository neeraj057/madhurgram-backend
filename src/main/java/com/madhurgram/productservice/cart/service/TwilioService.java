package com.madhurgram.productservice.cart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service client for executing outbound communications (such as WhatsApp reminders) 
 * via the Twilio SMS/WhatsApp REST API.
 */
@Slf4j
@Service
public class TwilioService {

    private static final String WHATSAPP_PREFIX = "whatsapp:";
    private static final String MOCK_SID_PREFIX = "ACmock";
    private static final String TWILIO_API_URL_TEMPLATE = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    
    private static final String PARAM_TO = "To";
    private static final String PARAM_FROM = "From";
    private static final String PARAM_BODY = "Body";

    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String COLON = ":";
    private static final String AND_SEPARATOR = "&";
    private static final String EQUALS = "=";
    
    private static final String INDIAN_PHONE_PREFIX = "+91";
    private static final String PLUS_PREFIX = "+";
    private static final String NON_DIGIT_REGEX = "\\D";

    private static final int TIMEOUT_SECONDS = 10;

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final HttpClient httpClient;

    /**
     * Constructor injection for TwilioService.
     * Configures request connection timeouts for production stability.
     *
     * @param accountSid twilio account sid
     * @param authToken  twilio authorization token
     * @param fromNumber registered twilio source sender number
     */
    public TwilioService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        
        // Connect timeout prevents infinite thread blocking if Twilio server is unreachable
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Sends an automated WhatsApp text message to a client phone.
     * If credentials start with ACmock, falls back to local logging mode.
     *
     * @param toPhoneNumber recipient phone number
     * @param messageText   body text layout
     */
    public void sendWhatsAppMessage(String toPhoneNumber, String messageText) {
        if (toPhoneNumber == null || toPhoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient phone number cannot be empty.");
        }
        if (messageText == null || messageText.trim().isEmpty()) {
            throw new IllegalArgumentException("Message body content cannot be empty.");
        }

        // Normalize target phone number
        String cleanPhone = toPhoneNumber.replaceAll(NON_DIGIT_REGEX, "");
        String formattedTo = cleanPhone.length() == 10 ? INDIAN_PHONE_PREFIX + cleanPhone : PLUS_PREFIX + cleanPhone;
        
        String twilioTo = WHATSAPP_PREFIX + formattedTo;
        String twilioFrom = WHATSAPP_PREFIX + fromNumber;

        // Check if Twilio configuration is in Mock Mode for local DEV environments
        if (accountSid == null || accountSid.trim().isEmpty() || accountSid.startsWith(MOCK_SID_PREFIX)) {
            log.info("[MOCK TWILIO WHATSAPP DISPATCH] Successfully triggered in DEV mode.");
            log.info("From: {}", twilioFrom);
            log.info("To: {}", twilioTo);
            log.info("Message Body:\n{}", messageText);
            return;
        }

        try {
            log.info("Sending live WhatsApp message via Twilio API to: {}", twilioTo);
            String url = String.format(TWILIO_API_URL_TEMPLATE, accountSid);
            
            Map<String, String> formData = Map.of(
                PARAM_TO, twilioTo,
                PARAM_FROM, twilioFrom,
                PARAM_BODY, messageText
            );

            String requestBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + EQUALS + 
                          URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining(AND_SEPARATOR));

            String authHeader = BASIC_AUTH_PREFIX + Base64.getEncoder().encodeToString(
                (accountSid + COLON + authToken).getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS)) // Read timeout limits request hangs
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Twilio WhatsApp reminder dispatched successfully! Response Status: {}", response.statusCode());
            } else {
                log.error("Twilio API rejected the request. Status Code: {}, Response: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Twilio API returned failure status " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message via Twilio API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to dispatch WhatsApp message via Twilio API: " + e.getMessage(), e);
        }
    }
}
