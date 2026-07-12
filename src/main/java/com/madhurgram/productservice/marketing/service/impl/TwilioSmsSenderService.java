package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.entity.BroadcastCampaign;
import com.madhurgram.productservice.marketing.repository.BroadcastCampaignRepository;
import com.madhurgram.productservice.marketing.service.SmsSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * Service implementation for dispatching promotional broadcast SMS campaigns 
 * using the Twilio REST API, supporting concurrent asynchronous execution.
 */
@Slf4j
@Service
public class TwilioSmsSenderService implements SmsSenderService {

    private static final String MOCK_SID_PREFIX = "ACmock";
    private static final String TWILIO_API_URL_TEMPLATE = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    
    private static final String PARAM_TO = "To";
    private static final String PARAM_FROM = "From";
    private static final String PARAM_BODY = "Body";

    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String COLON = ":";
    private static final String AND_SEPARATOR = "&";
    private static final String EQUALS = "=";

    private static final int TIMEOUT_SECONDS = 10;

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final HttpClient httpClient;
    private final BroadcastCampaignRepository campaignRepository;

    /**
     * Constructor injection for TwilioSmsSenderService.
     * Configures connect timeouts on the HTTP client to safeguard server thread pools.
     *
     * @param accountSid         twilio account identifier
     * @param authToken          twilio secret authentication token
     * @param fromNumber         source messaging phone number
     * @param campaignRepository broadcast campaigns queue repository
     */
    public TwilioSmsSenderService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber,
            BroadcastCampaignRepository campaignRepository
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.campaignRepository = campaignRepository;
        
        // Connect timeout prevents infinite thread blocking if Twilio server is unreachable
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        if (accountSid == null || accountSid.isBlank() || authToken == null || 
                authToken.isBlank() || fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio configuration is missing. Configure twilio.account-sid, twilio.auth-token, and twilio.from-number.");
        }
    }

    /**
     * Performs a synchronous broadcast of SMS dispatches.
     *
     * @param recipients list of phone numbers
     * @param message    outbound SMS text message body
     * @return successful message count sent
     */
    @Override
    public int sendBroadcastMessage(List<String> recipients, String message) {
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }

        // Check if Twilio configuration is in Mock Mode for local DEV environments
        if (accountSid.trim().startsWith(MOCK_SID_PREFIX)) {
            log.info("[MOCK TWILIO BROADCAST DISPATCH] Successfully triggered mock broadcast for {} recipients in DEV mode.", recipients.size());
            log.info("Mock message: '{}'", message);
            return recipients.size();
        }

        String endpoint = String.format(TWILIO_API_URL_TEMPLATE, accountSid);
        String authorizationHeader = BASIC_AUTH_PREFIX + Base64.getEncoder().encodeToString(
                (accountSid + COLON + authToken).getBytes(StandardCharsets.UTF_8)
        );

        int successCount = 0;
        for (String recipient : recipients) {
            try {
                if (recipient == null || recipient.isBlank()) {
                    log.warn("Skipping blank recipient phone number for broadcast message.");
                    continue;
                }

                String body = buildForm(PARAM_TO, recipient)
                        + AND_SEPARATOR + buildForm(PARAM_FROM, fromNumber)
                        + AND_SEPARATOR + buildForm(PARAM_BODY, message);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS)) // Read timeout limits request hangs
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    successCount++;
                } else {
                    log.warn("Twilio send failed for {}. status={} response={}", recipient, response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.error("Failed to send Twilio SMS to recipient {}", recipient, e);
            }
        }

        log.info("Broadcast message sent to {} recipients out of {} attempted.", successCount, recipients.size());
        return successCount;
    }

    private String buildForm(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + EQUALS + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Executes asynchronous broadcast campaign dispatch mapping sent count results back to the database.
     *
     * @param campaignId target database campaign identifier
     * @param recipients recipients list
     * @param message    outbound SMS text message body
     */
    @Override
    @Async
    public void sendBroadcastMessageAsync(Long campaignId, List<String> recipients, String message) {
        log.info("Starting asynchronous broadcast for campaign ID: {} to {} recipients", campaignId, recipients.size());
        int sentCount = sendBroadcastMessage(recipients, message);
        try {
            BroadcastCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign != null) {
                campaign.setRecipients(sentCount);
                campaignRepository.save(campaign);
                log.info("Updated campaign ID: {} with sent count: {}", campaignId, sentCount);
            }
        } catch (Exception e) {
            log.error("Failed to update campaign send status for campaign ID: {}", campaignId, e);
        }
    }
}
