package com.bank.bankmanagement.auth.dto;

import com.bank.bankmanagement.user.enums.UserRole;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String email,
        UserRole role
) {
}
