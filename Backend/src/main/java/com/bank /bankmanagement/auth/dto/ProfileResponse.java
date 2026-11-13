package com.bank.bankmanagement.auth.dto;

import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;

public record ProfileResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String address,
        UserRole role,
        KycStatus kycStatus
) {}
