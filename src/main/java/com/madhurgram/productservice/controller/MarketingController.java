package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.service.MarketingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/marketing")
@CrossOrigin(origins = "*")
public class MarketingController {

    private final MarketingService marketingService;

    public MarketingController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastCampaignDTO> broadcastCampaign(@RequestBody BroadcastCampaignRequestDTO request) {
        BroadcastCampaignDTO campaign = marketingService.createCampaign(request);
        return ResponseEntity.ok(campaign);
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<BroadcastCampaignDTO>> getCampaigns() {
        return ResponseEntity.ok(marketingService.getCampaignSummaries());
    }
}
