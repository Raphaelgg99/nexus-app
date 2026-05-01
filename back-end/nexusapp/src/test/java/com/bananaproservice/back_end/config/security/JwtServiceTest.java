package com.nexusapp.back_end.config.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nexusapp.back_end.user.model.User;
import com.nexusapp.back_end.user.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void generateTokenAndExtractSubjectShouldWorkTogether() {
        JwtService service = new JwtService("secret-key", 60_000L, "Bearer");
        User user = new User("admin", UserRole.ADMIN, "encoded-password");

        String token = service.generateToken(user);

        assertEquals("admin", service.extractSubject(token));
        assertEquals("Bearer", service.getTokenPrefix());
        assertEquals(60_000L, service.getExpirationMillis());
    }

    @Test
    void isTokenValidShouldReturnTrueForMatchingUserNameIgnoringCase() {
        JwtService service = new JwtService("secret-key", 60_000L, "Bearer");
        User user = new User("admin", UserRole.ADMIN, "encoded-password");
        String token = service.generateToken(user);
        UserDetails details = new org.springframework.security.core.userdetails.User(
                "ADMIN",
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertTrue(service.isTokenValid(token, details));
    }

    @Test
    void isTokenValidShouldReturnFalseForDifferentUserName() {
        JwtService service = new JwtService("secret-key", 60_000L, "Bearer");
        User user = new User("admin", UserRole.ADMIN, "encoded-password");
        String token = service.generateToken(user);
        UserDetails details = new org.springframework.security.core.userdetails.User(
                "banana",
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertFalse(service.isTokenValid(token, details));
    }

    @Test
    void extractSubjectShouldThrowForInvalidToken() {
        JwtService service = new JwtService("secret-key", 60_000L, "Bearer");

        assertThrows(JWTVerificationException.class, () -> service.extractSubject("invalid-token"));
    }
}
