package com.bank.bankmanagement.account.dto;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.enums.AccountStatus;
import com.bank.bankmanagement.account.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        BigDecimal minimumBalance,
        BigDecimal annualInterestRate,
        AccountStatus status,
        Integer tenureMonths,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        Integer tenure = account.getAccountType() == AccountType.FIXED_DEPOSIT
                ? ((com.bank.bankmanagement.account.entity.FixedDepositAccount) account)
                    .getTenureMonths()
                : null;

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getMinimumBalance(),
                account.getAnnualInterestRate(),
                account.getStatus(),
                tenure,
                account.getCreatedAt()
        );
    }
}
