package com.bank.bankmanagement.admin.dto;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;

import java.time.LocalDateTime;

public record StaffResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String nationalId,
        String address,
        UserRole role,
        boolean enabled,
        LocalDateTime createdAt
) {
    public static StaffResponse from(BankUser user) {
        return new StaffResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getNationalId(), user.getAddress(),
                user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
