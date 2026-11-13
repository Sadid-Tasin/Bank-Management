package com.bank.bankmanagement.employee.dto;

import com.bank.bankmanagement.user.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KycDecisionRequest(
        @NotNull(message = "KYC decision is required")
        KycStatus status,

        @Size(max = 500, message = "Review note cannot exceed 500 characters")
        String note
) {}
