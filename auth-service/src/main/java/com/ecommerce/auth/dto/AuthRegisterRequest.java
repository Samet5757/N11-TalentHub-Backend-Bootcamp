package com.ecommerce.auth.dto;

import com.ecommerce.auth.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        String username,
        @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
        String password,
        @NotNull(message = "role is required")
        UserRole role
) {
}
