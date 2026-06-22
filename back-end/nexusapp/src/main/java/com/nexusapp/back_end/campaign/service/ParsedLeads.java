package com.nexusapp.back_end.campaign.service;

import com.nexusapp.back_end.campaign.dto.CampaignLead;

import java.util.List;

public record ParsedLeads(
        int totalRows,
        List<CampaignLead> validLeads,
        int invalidLeads,
        int duplicateLeads,
        List<String> errors
) {
}
