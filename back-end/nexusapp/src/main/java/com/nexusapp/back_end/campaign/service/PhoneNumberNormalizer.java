package com.nexusapp.back_end.campaign.service;

import org.springframework.stereotype.Component;

@Component
public class PhoneNumberNormalizer {

    public String normalizeBrazilianWhatsapp(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Phone is required.");
        }

        String trimmedPhone = rawPhone.trim();
        if (trimmedPhone.startsWith("+") && !trimmedPhone.startsWith("+55")) {
            throw new IllegalArgumentException("Phone must be a Brazilian number.");
        }

        String digits = rawPhone.replaceAll("\\D", "");

        if (digits.length() == 10 || digits.length() == 11) {
            digits = "55" + digits;
        }

        if (!digits.startsWith("55")) {
            throw new IllegalArgumentException("Phone must be a Brazilian number.");
        }

        int nationalLength = digits.length() - 2;
        if (nationalLength != 10 && nationalLength != 11) {
            throw new IllegalArgumentException("Phone must have 10 or 11 Brazilian digits after country code.");
        }

        String ddd = digits.substring(2, 4);
        if (ddd.startsWith("0")) {
            throw new IllegalArgumentException("Phone DDD is invalid.");
        }

        return digits;
    }
}
