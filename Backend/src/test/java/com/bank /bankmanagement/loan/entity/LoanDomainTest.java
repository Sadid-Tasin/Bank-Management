package com.bank.bankmanagement.loan.entity;

import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.loan.enums.LoanStatus;
import com.bank.bankmanagement.loan.enums.LoanType;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class LoanDomainTest {
    private Loan newLoan(String principal, int months) {
        BankUser customer = new BankUser("Loan Tester", "loan@test.example", "hash", UserRole.CUSTOMER);
        SavingsAccount account = new SavingsAccount("LOAN-TEST-001", customer,
                new BigDecimal("10000.00"), new BigDecimal("500.00"), new BigDecimal("5.00"));
        return new Loan(customer, account, LoanType.PERSONAL, "Education expenses",
                new BigDecimal(principal), months);
    }

    @Test void emiAtZeroInterestUsesPrincipalDividedByTerm() {
        assertEquals(new BigDecimal("833.33"), Loan.calculateEmi(
                new BigDecimal("10000.00"), BigDecimal.ZERO, 12));
    }

    @Test void emiAtPositiveRateIsRoundedToTwoDecimals() {
        BigDecimal emi = Loan.calculateEmi(new BigDecimal("10000.00"), new BigDecimal("12.00"), 12);
        assertEquals(2, emi.scale());
        assertTrue(emi.compareTo(new BigDecimal("888.00")) > 0);
    }

    @Test void newLoanStartsPendingAndApprovalCalculatesRepayment() {
        Loan loan = newLoan("3000.00", 3);
        assertEquals(LoanStatus.PENDING, loan.getStatus());
        loan.approve(BigDecimal.ZERO, "officer@test.example", "Approved");
        assertEquals(LoanStatus.DISBURSED, loan.getStatus());
        assertEquals(new BigDecimal("1000.00"), loan.getMonthlyInstallment());
        assertEquals(new BigDecimal("3000.00"), loan.getOutstandingAmount());
    }

    @Test void rejectionRequiresReasonAndCannotBeReapproved() {
        Loan loan = newLoan("3000.00", 3);
        assertThrows(IllegalArgumentException.class,
                () -> loan.reject("officer@test.example", " "));
        loan.reject("officer@test.example", "Insufficient supporting documents");
        assertEquals(LoanStatus.REJECTED, loan.getStatus());
        assertThrows(IllegalStateException.class,
                () -> loan.approve(new BigDecimal("10.00"), "other@test.example", null));
    }

    @Test void finalInstallmentAdjustsForRoundingAndPaysLoanOff() {
        Loan loan = newLoan("1000.00", 3);
        loan.approve(BigDecimal.ZERO, "officer@test.example", "Approved");
        loan.recordRepayment(new BigDecimal("333.33"), 1);
        loan.recordRepayment(new BigDecimal("333.33"), 2);
        assertEquals(new BigDecimal("333.34"), loan.getOutstandingAmount());
        loan.recordRepayment(new BigDecimal("333.34"), 3);
        assertEquals(LoanStatus.PAID_OFF, loan.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), loan.getOutstandingAmount());
    }
}
