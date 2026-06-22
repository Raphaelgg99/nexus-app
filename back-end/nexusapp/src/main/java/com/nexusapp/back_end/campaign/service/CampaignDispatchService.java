package com.nexusapp.back_end.campaign.service;

import com.nexusapp.back_end.campaign.dto.CampaignDispatchResponse;
import com.nexusapp.back_end.campaign.dto.N8nCampaignDispatchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;

@Service
public class CampaignDispatchService {

    private static final int DEFAULT_DELAY_SECONDS = 10;
    private static final int MIN_DELAY_SECONDS = 1;
    private static final int MAX_DELAY_SECONDS = 300;

    private final LeadCsvParser leadCsvParser;
    private final WebClient webClient;
    private final String webhookUrl;
    private final long maxFileSizeBytes;

    public CampaignDispatchService(
            LeadCsvParser leadCsvParser,
            WebClient.Builder webClientBuilder,
            @Value("${n8n.campaign.webhook-url:}") String webhookUrl,
            @Value("${campaign.leads.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.leadCsvParser = leadCsvParser;
        this.webClient = webClientBuilder.build();
        this.webhookUrl = webhookUrl;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public CampaignDispatchResponse dispatch(
            String campaignName,
            String message,
            Integer delayBetweenMessages,
            MultipartFile leadsFile
    ) {
        validateText(campaignName, "Campaign name is required.");
        validateText(message, "Message is required.");
        validateFile(leadsFile);

        int resolvedDelay = resolveDelay(delayBetweenMessages);
        ParsedLeads parsedLeads;
        try {
            parsedLeads = leadCsvParser.parse(leadsFile.getInputStream());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read leads file.");
        }

        if (parsedLeads.validLeads().isEmpty()) {
            throw new IllegalArgumentException("Leads file does not contain valid leads.");
        }

        sendToN8n(new N8nCampaignDispatchRequest(
                campaignName.trim(),
                message.trim(),
                resolvedDelay,
                parsedLeads.validLeads()
        ));

        return new CampaignDispatchResponse(
                campaignName.trim(),
                parsedLeads.totalRows(),
                parsedLeads.validLeads().size(),
                parsedLeads.invalidLeads(),
                parsedLeads.duplicateLeads(),
                resolvedDelay,
                true,
                parsedLeads.errors()
        );
    }

    private void sendToN8n(N8nCampaignDispatchRequest request) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalStateException("N8N campaign webhook URL is not configured.");
        }

        webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(30));
    }

    private int resolveDelay(Integer delayBetweenMessages) {
        int resolvedDelay = delayBetweenMessages == null ? DEFAULT_DELAY_SECONDS : delayBetweenMessages;
        if (resolvedDelay < MIN_DELAY_SECONDS || resolvedDelay > MAX_DELAY_SECONDS) {
            throw new IllegalArgumentException("Delay between messages must be between 1 and 300 seconds.");
        }

        return resolvedDelay;
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateFile(MultipartFile leadsFile) {
        if (leadsFile == null || leadsFile.isEmpty()) {
            throw new IllegalArgumentException("Leads file is required.");
        }

        if (leadsFile.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Leads file exceeds the maximum allowed size.");
        }

        String filename = leadsFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Leads file must be a CSV file.");
        }
    }
}
