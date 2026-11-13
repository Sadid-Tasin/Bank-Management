package com.bank.bankmanagement.account.dto;

import com.bank.bankmanagement.account.enums.AccountStatus;

import java.math.BigDecimal;

public record BalanceResponse(
        String accountNumber,
        BigDecimal balance,
        BigDecimal minimumBalance,
        AccountStatus status
) {}
