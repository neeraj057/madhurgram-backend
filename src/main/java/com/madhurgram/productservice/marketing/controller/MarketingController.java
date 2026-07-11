package com.madhurgram.productservice.marketing.controller;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.marketing.service.MarketingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/marketing")
public class MarketingController {

    private final MarketingService marketingService;

    public MarketingController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastCampaignDTO> broadcastCampaign(@Valid @RequestBody BroadcastCampaignRequestDTO request) {
        BroadcastCampaignDTO campaign = marketingService.createCampaign(request);
        return ResponseEntity.ok(campaign);
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<BroadcastCampaignDTO>> getCampaigns() {
        return ResponseEntity.ok(marketingService.getCampaignSummaries());
    }
}
