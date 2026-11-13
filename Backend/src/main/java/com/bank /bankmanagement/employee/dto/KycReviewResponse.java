package com.bank.bankmanagement.employee.dto;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;

import java.time.LocalDateTime;

public record KycReviewResponse(
        Long customerId,
        String fullName,
        String email,
        String phone,
        String nationalId,
        String address,
        KycStatus kycStatus,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewedByEmail,
        String reviewNote
) {
    public static KycReviewResponse from(BankUser user) {
        return new KycReviewResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getNationalId(), user.getAddress(), user.getKycStatus(),
                user.getCreatedAt(), user.getKycReviewedAt(),
                user.getKycReviewedByEmail(), user.getKycReviewNote());
    }
}
