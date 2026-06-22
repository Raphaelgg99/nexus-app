package com.nexusapp.back_end.campaign.controller;

import com.nexusapp.back_end.campaign.dto.CampaignDispatchResponse;
import com.nexusapp.back_end.campaign.service.CampaignDispatchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignDispatchController {

    private final CampaignDispatchService campaignDispatchService;

    public CampaignDispatchController(CampaignDispatchService campaignDispatchService) {
        this.campaignDispatchService = campaignDispatchService;
    }

    @PostMapping(value = "/dispatch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CampaignDispatchResponse> dispatch(
            @RequestParam String campaignName,
            @RequestParam String message,
            @RequestParam(required = false) Integer delayBetweenMessages,
            @RequestParam MultipartFile leadsFile
    ) {
        return ResponseEntity.ok(campaignDispatchService.dispatch(
                campaignName,
                message,
                delayBetweenMessages,
                leadsFile
        ));
    }
}
