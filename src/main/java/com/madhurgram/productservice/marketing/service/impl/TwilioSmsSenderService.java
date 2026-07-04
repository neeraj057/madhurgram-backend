package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.entity.BroadcastCampaign;
import com.madhurgram.productservice.marketing.repository.BroadcastCampaignRepository;
import com.madhurgram.productservice.marketing.service.SmsSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
    private final BroadcastCampaignRepository campaignRepository;

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

    @Override
    @Async
    public void sendBroadcastMessageAsync(Long campaignId, List<String> recipients, String message) {
        logger.info("Starting asynchronous broadcast for campaign ID: {} to {} recipients", campaignId, recipients.size());
        int sentCount = sendBroadcastMessage(recipients, message);
        try {
            BroadcastCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign != null) {
                campaign.setRecipients(sentCount);
                campaignRepository.save(campaign);
                logger.info("Updated campaign ID: {} with sent count: {}", campaignId, sentCount);
            }
        } catch (Exception e) {
            logger.error("Failed to update campaign send status for campaign ID: {}", campaignId, e);
        }
    }
}
