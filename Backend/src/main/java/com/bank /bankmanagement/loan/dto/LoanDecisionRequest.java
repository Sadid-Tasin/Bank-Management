package com.bank.bankmanagement.loan.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record LoanDecisionRequest(
    @NotNull Boolean approved,
    @DecimalMin("0.00") @DecimalMax("40.00") @Digits(integer=2, fraction=2) BigDecimal annualInterestRate,
    @Size(max=500) String note
) {}
