package com.bank.bankmanagement.transaction.entity;

import com.bank.bankmanagement.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String reference,
        TransactionType type,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(BankTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getType(),
                transaction.getSourceAccount() == null
                        ? null : transaction.getSourceAccount().getAccountNumber(),
                transaction.getDestinationAccount() == null
                        ? null : transaction.getDestinationAccount().getAccountNumber(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
