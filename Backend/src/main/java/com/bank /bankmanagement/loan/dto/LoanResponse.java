package com.bank.bankmanagement.loan.dto;
import com.bank.bankmanagement.loan.entity.Loan;
import com.bank.bankmanagement.loan.enums.LoanStatus;
import com.bank.bankmanagement.loan.enums.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record LoanResponse(Long id, String loanNumber, String applicantEmail, String accountNumber,
    LoanType loanType, String purpose, BigDecimal principalAmount, BigDecimal annualInterestRate,
    Integer termMonths, LoanStatus status, BigDecimal monthlyInstallment, BigDecimal totalPayable,
    BigDecimal outstandingAmount, LocalDate nextDueDate, String reviewedByEmail,
    String decisionNote, LocalDateTime submittedAt, LocalDateTime reviewedAt, LocalDateTime disbursedAt) {
    public static LoanResponse from(Loan l) { return new LoanResponse(l.getId(),l.getLoanNumber(),l.getApplicant().getEmail(),
        l.getLinkedAccount().getAccountNumber(),l.getLoanType(),l.getPurpose(),l.getPrincipalAmount(),l.getAnnualInterestRate(),
        l.getTermMonths(),l.getStatus(),l.getMonthlyInstallment(),l.getTotalPayable(),l.getOutstandingAmount(),
        l.getNextDueDate(),l.getReviewedByEmail(),l.getDecisionNote(),l.getSubmittedAt(),l.getReviewedAt(),l.getDisbursedAt()); }
}
