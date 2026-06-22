package com.nexusapp.back_end.campaign.dto;

import java.util.List;

public record N8nCampaignDispatchRequest(
        String campaignName,
        String message,
        int delayBetweenMessages,
        List<CampaignLead> leads
) {
}
