package com.bank.bankmanagement.admin.dto;

import com.bank.bankmanagement.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record StaffRoleUpdateRequest(
        @NotNull(message = "Staff role is required")
        UserRole role
) {}
