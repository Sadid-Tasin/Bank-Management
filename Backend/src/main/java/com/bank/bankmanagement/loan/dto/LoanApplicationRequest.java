package com.bank.bankmanagement.loan.dto;
import com.bank.bankmanagement.loan.enums.LoanType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record LoanApplicationRequest(
    @NotNull LoanType loanType,
    @NotBlank @Size(max=500) String purpose,
    @NotNull @DecimalMin("1000.00") @Digits(integer=17, fraction=2) BigDecimal principalAmount,
    @Min(3) @Max(84) int termMonths,
    @NotBlank @Size(max=30) String accountNumber
) {}
