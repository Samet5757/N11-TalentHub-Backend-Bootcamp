package com.ecommerce.auth.dto;

import com.ecommerce.auth.entity.UserRole;

public record AuthUserResponse(
        Long id,
        String username,
        UserRole role
) {
}
