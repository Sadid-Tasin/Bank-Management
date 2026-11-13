package com.bank.bankmanagement.admin.dto;

import com.bank.bankmanagement.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffCreateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be 8 to 64 characters")
        String password,

        @Size(max = 20)
        String phone,

        @Size(max = 30)
        String nationalId,

        @Size(max = 255)
        String address,

        @NotNull(message = "Staff role is required")
        UserRole role
) {}
