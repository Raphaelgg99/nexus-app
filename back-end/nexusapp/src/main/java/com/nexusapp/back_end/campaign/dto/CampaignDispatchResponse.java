package com.nexusapp.back_end.campaign.dto;

import java.util.List;

public record CampaignDispatchResponse(
        String campaignName,
        int totalRows,
        int validLeads,
        int invalidLeads,
        int duplicateLeads,
        int delayBetweenMessages,
        boolean sentToN8n,
        List<String> errors
) {
}
