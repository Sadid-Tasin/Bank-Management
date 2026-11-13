package com.bank.bankmanagement.loan.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record LoanRepaymentRequest(@NotBlank @Size(max=30) String accountNumber,
    @NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal amount) {}
