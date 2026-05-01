package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
        String password,
        @NotBlank(message = "firstName is required")
        @Size(min = 2, max = 50, message = "firstName must be between 2 and 50 characters")
        String firstName,
        @NotBlank(message = "lastName is required")
        @Size(min = 2, max = 50, message = "lastName must be between 2 and 50 characters")
        String lastName
) {
}
