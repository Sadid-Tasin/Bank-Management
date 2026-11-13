package com.bank.bankmanagement.account.dto;

import com.bank.bankmanagement.account.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotNull(message = "Opening balance is required")
        @DecimalMin(value = "0.01", message = "Opening balance must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Opening balance supports at most two decimal places")
        BigDecimal openingBalance,

        @Min(value = 1, message = "Fixed deposit tenure must be at least one month")
        @Max(value = 60, message = "Fixed deposit tenure cannot exceed 60 months")
        Integer tenureMonths
) {}
