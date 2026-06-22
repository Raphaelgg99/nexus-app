package com.bananaproservice.back_end.campaign.service;

import com.nexusapp.back_end.campaign.dto.CampaignLead;
import com.nexusapp.back_end.campaign.service.LeadCsvParser;
import com.nexusapp.back_end.campaign.service.ParsedLeads;
import com.nexusapp.back_end.campaign.service.PhoneNumberNormalizer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeadCsvParserTest {

    private final LeadCsvParser parser = new LeadCsvParser(new PhoneNumberNormalizer());

    @Test
    void shouldParseValidLeadsAndRemoveDuplicates() {
        String csv = """
                nome,telefone
                Rafael,(11) 99999-9999
                Maria,+55 21 98888-7777
                Duplicado,5511999999999
                """;

        ParsedLeads result = parser.parse(stream(csv));

        assertEquals(3, result.totalRows());
        assertEquals(2, result.validLeads().size());
        assertEquals(0, result.invalidLeads());
        assertEquals(1, result.duplicateLeads());
        assertEquals(List.of(
                new CampaignLead("Rafael", "5511999999999"),
                new CampaignLead("Maria", "5521988887777")
        ), result.validLeads());
    }

    @Test
    void shouldAcceptSemicolonSeparatedCsvAndPhoneAliases() {
        String csv = """
                name;wpp
                Joao;11988887777
                """;

        ParsedLeads result = parser.parse(stream(csv));

        assertEquals(1, result.validLeads().size());
        assertEquals(new CampaignLead("Joao", "5511988887777"), result.validLeads().get(0));
    }

    @Test
    void shouldTrackInvalidRows() {
        String csv = """
                nome,telefone
                Sem telefone,
                ,11999999999
                """;

        ParsedLeads result = parser.parse(stream(csv));

        assertEquals(2, result.totalRows());
        assertEquals(0, result.validLeads().size());
        assertEquals(2, result.invalidLeads());
        assertEquals(2, result.errors().size());
    }

    @Test
    void shouldRejectMissingRequiredColumns() {
        String csv = """
                email,documento
                teste@teste.com,123
                """;

        assertThrows(IllegalArgumentException.class, () -> parser.parse(stream(csv)));
    }

    private ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
