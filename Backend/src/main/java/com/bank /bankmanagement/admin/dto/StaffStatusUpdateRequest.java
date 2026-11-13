package com.bank.bankmanagement.admin.dto;

import jakarta.validation.constraints.NotNull;

public record StaffStatusUpdateRequest(
        @NotNull(message = "Enabled status is required")
        Boolean enabled
) {}
