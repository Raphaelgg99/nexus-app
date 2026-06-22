package com.nexusapp.back_end.campaign.service;

import com.nexusapp.back_end.campaign.dto.CampaignLead;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class LeadCsvParser {

    private final PhoneNumberNormalizer phoneNumberNormalizer;

    public LeadCsvParser(PhoneNumberNormalizer phoneNumberNormalizer) {
        this.phoneNumberNormalizer = phoneNumberNormalizer;
    }

    public ParsedLeads parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("Leads file is empty.");
            }

            char delimiter = detectDelimiter(headerLine);
            List<String> headers = parseLine(headerLine, delimiter);
            int nameIndex = findHeader(headers, "nome", "name");
            int phoneIndex = findHeader(headers, "telefone", "phone", "numero", "número", "whatsapp", "wpp");

            if (nameIndex < 0 || phoneIndex < 0) {
                throw new IllegalArgumentException("Leads file must contain name/nome and phone/telefone columns.");
            }

            List<CampaignLead> validLeads = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            Set<String> phones = new HashSet<>();
            int totalRows = 0;
            int invalidLeads = 0;
            int duplicateLeads = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                totalRows++;
                List<String> columns = parseLine(line, delimiter);
                String name = getColumn(columns, nameIndex);
                String phone = getColumn(columns, phoneIndex);

                try {
                    if (name.isBlank()) {
                        throw new IllegalArgumentException("Name is required.");
                    }

                    String normalizedPhone = phoneNumberNormalizer.normalizeBrazilianWhatsapp(phone);
                    if (!phones.add(normalizedPhone)) {
                        duplicateLeads++;
                        continue;
                    }

                    validLeads.add(new CampaignLead(name.trim(), normalizedPhone));
                } catch (IllegalArgumentException exception) {
                    invalidLeads++;
                    errors.add("Line " + (totalRows + 1) + ": " + exception.getMessage());
                }
            }

            return new ParsedLeads(totalRows, validLeads, invalidLeads, duplicateLeads, errors);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read leads file.");
        }
    }

    private char detectDelimiter(String headerLine) {
        long semicolons = headerLine.chars().filter(character -> character == ';').count();
        long commas = headerLine.chars().filter(character -> character == ',').count();

        return semicolons > commas ? ';' : ',';
    }

    private int findHeader(List<String> headers, String... candidates) {
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index).trim().toLowerCase(Locale.ROOT);
            for (String candidate : candidates) {
                if (header.equals(candidate)) {
                    return index;
                }
            }
        }

        return -1;
    }

    private String getColumn(List<String> columns, int index) {
        if (index >= columns.size()) {
            return "";
        }

        return columns.get(index).trim();
    }

    private List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);

            if (character == '"') {
                boolean escapedQuote = insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"';
                if (escapedQuote) {
                    current.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == delimiter && !insideQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString());
        return values;
    }
}
