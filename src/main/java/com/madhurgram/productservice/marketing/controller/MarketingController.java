package com.madhurgram.productservice.marketing.controller;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.marketing.service.MarketingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for dispatching manual broadcast campaigns to customers.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/marketing")
@Tag(name = "Admin — Marketing Campaigns", description = "Endpoints for scheduling and broadcasting manual marketing campaigns")
public class MarketingController {

    private final MarketingService marketingService;

    /**
     * Constructor injection for MarketingController.
     *
     * @param marketingService campaign management service
     */
    public MarketingController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    /**
     * Triggers a manual broadcast push campaign.
     *
     * @param request campaign details like templates and targeting customer segment filters
     * @return summary details of the created campaign
     */
    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast push campaign", description = "Launches a marketing campaign to segmented audiences immediately.")
    public ResponseEntity<BroadcastCampaignDTO> broadcastCampaign(@Valid @RequestBody BroadcastCampaignRequestDTO request) {
        log.info("Admin request: launch campaign '{}' (Segment: '{}')", request.title(), request.targetSegment());
        BroadcastCampaignDTO campaign = marketingService.createCampaign(request);
        log.info("Campaign '{}' successfully created and scheduled with ID: {}", campaign.title(), campaign.id());
        return ResponseEntity.ok(campaign);
    }

    /**
     * Retrieves records of past campaigns.
     *
     * @return a list of campaign summaries
     */
    @GetMapping("/campaigns")
    @Operation(summary = "List marketing campaigns", description = "Retrieves status, statistics, and run logs of all past campaigns.")
    public ResponseEntity<List<BroadcastCampaignDTO>> getCampaigns() {
        log.info("Admin request: list all campaigns");
        List<BroadcastCampaignDTO> campaigns = marketingService.getCampaignSummaries();
        log.info("Returning {} campaign summary/summaries", campaigns.size());
        return ResponseEntity.ok(campaigns);
    }
}
