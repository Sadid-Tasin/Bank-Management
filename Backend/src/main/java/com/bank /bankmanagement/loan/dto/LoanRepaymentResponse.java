package com.bank.bankmanagement.loan.dto;
import com.bank.bankmanagement.loan.entity.LoanRepayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record LoanRepaymentResponse(String reference, Long loanId, String accountNumber,
    BigDecimal amount, Integer installmentNumber, LocalDateTime paidAt) {
    public static LoanRepaymentResponse from(LoanRepayment r) { return new LoanRepaymentResponse(r.getReference(),r.getLoan().getId(),
        r.getPayingAccount().getAccountNumber(),r.getAmount(),r.getInstallmentNumber(),r.getPaidAt()); }
}
