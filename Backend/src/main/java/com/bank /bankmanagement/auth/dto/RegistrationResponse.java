package com.bank.bankmanagement.auth.dto;

import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;

public record RegistrationResponse(
        Long userId,
        String email,
        UserRole role,
        KycStatus kycStatus
) {
}
