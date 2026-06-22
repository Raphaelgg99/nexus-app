package com.bananaproservice.back_end.campaign.service;

import com.nexusapp.back_end.campaign.service.PhoneNumberNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberNormalizerTest {

    private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

    @Test
    void shouldNormalizeLocalMobileNumber() {
        assertEquals("5511999999999", normalizer.normalizeBrazilianWhatsapp("(11) 99999-9999"));
    }

    @Test
    void shouldKeepBrazilianCountryCodeWhenPresent() {
        assertEquals("5521988887777", normalizer.normalizeBrazilianWhatsapp("+55 21 98888-7777"));
    }

    @Test
    void shouldRejectNumbersOutsideBrazil() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeBrazilianWhatsapp("+1 202 555 0199"));
    }

    @Test
    void shouldRejectInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeBrazilianWhatsapp("119999"));
    }
}
