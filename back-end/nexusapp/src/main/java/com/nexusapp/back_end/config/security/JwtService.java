package com.nexusapp.back_end.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nexusapp.back_end.user.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String ISSUER = "nexusapp";

    private final Algorithm algorithm;
    private final long expirationMillis;
    private final String tokenPrefix;

    public JwtService(
            @Value("${api.security.token.secrets}") String secret,
            @Value("${security.config.expiration}") long expirationMillis,
            @Value("${security.config.prefix:Bearer}") String tokenPrefix
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMillis = expirationMillis;
        this.tokenPrefix = tokenPrefix;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getUserName())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(expirationMillis)))
                .sign(algorithm);
    }

    public String extractSubject(String token) throws JWTVerificationException {
        return JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
                .verify(token)
                .getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractSubject(token).equalsIgnoreCase(userDetails.getUsername());
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }
}
