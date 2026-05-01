package com.nexusapp.back_end.user.dto;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        UserResponse user
) {
}
