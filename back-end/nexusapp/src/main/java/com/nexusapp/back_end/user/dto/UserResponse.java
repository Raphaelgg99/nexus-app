package com.nexusapp.back_end.user.dto;

import com.nexusapp.back_end.user.model.UserRole;

public record UserResponse(
        Long id,
        String userName,
        UserRole role
) {
}
