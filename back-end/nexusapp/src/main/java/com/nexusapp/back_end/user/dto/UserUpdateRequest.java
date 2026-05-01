package com.nexusapp.back_end.user.dto;

import com.nexusapp.back_end.user.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 120, message = "Username must have at most 120 characters")
        String userName,

        @NotNull(message = "Role is required")
        UserRole role,

        @Size(min = 6, max = 255, message = "Password must have between 6 and 255 characters")
        String password
) {
}
