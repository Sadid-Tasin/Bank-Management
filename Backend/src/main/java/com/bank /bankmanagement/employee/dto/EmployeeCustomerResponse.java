package com.bank.bankmanagement.employee.dto;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;

import java.time.LocalDateTime;

public record EmployeeCustomerResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String nationalId,
        String address,
        KycStatus kycStatus,
        boolean enabled,
        LocalDateTime createdAt
) {
    public static EmployeeCustomerResponse from(BankUser user) {
        return new EmployeeCustomerResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getNationalId(), user.getAddress(), user.getKycStatus(),
                user.isEnabled(), user.getCreatedAt());
    }
}
