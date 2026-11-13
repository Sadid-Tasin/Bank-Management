package com.bank.bankmanagement.account.entity;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountDomainTest {

    private BankUser createCustomer() {
        return new BankUser(
                "Test Customer",
                "customer@test.com",
                "hashed-password",
                UserRole.CUSTOMER
        );
    }

    @Test
    void savingsAccountShouldCalculateInterest() {
        SavingsAccount account = new SavingsAccount(
                "SAV1001", createCustomer(),
                new BigDecimal("1000"),
                new BigDecimal("500"),
                new BigDecimal("5")
        );

        assertEquals(
                new BigDecimal("50.00"),
                account.calculateInterest()
        );
    }

    @Test
    void depositAndWithdrawShouldUpdateBalance() {
        SavingsAccount account = new SavingsAccount(
                "SAV1002", createCustomer(),
                new BigDecimal("1000"),
                new BigDecimal("500"),
                new BigDecimal("5")
        );

        account.deposit(new BigDecimal("500"));
        account.withdraw(new BigDecimal("200"));

        assertEquals(
                new BigDecimal("1300.00"),
                account.getBalance()
        );
    }

    @Test
    void withdrawalShouldNotBreakMinimumBalance() {
        SavingsAccount account = new SavingsAccount(
                "SAV1003", createCustomer(),
                new BigDecimal("1000"),
                new BigDecimal("500"),
                new BigDecimal("5")
        );

        assertThrows(
                IllegalStateException.class,
                () -> account.withdraw(new BigDecimal("600"))
        );

        assertEquals(
                new BigDecimal("1000.00"),
                account.getBalance()
        );
    }

    @Test
    void accountShouldRejectZeroOrNegativeDeposit() {
        SavingsAccount account = new SavingsAccount(
                "SAV1004", createCustomer(),
                new BigDecimal("1000"),
                new BigDecimal("500"),
                new BigDecimal("5")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> account.deposit(BigDecimal.ZERO)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> account.deposit(new BigDecimal("-100"))
        );
    }

    @Test
    void frozenAccountShouldRejectDeposit() {
        SavingsAccount account = new SavingsAccount(
                "SAV1005", createCustomer(),
                new BigDecimal("1000"),
                new BigDecimal("500"),
                new BigDecimal("5")
        );

        account.freeze();

        assertThrows(
                IllegalStateException.class,
                () -> account.deposit(new BigDecimal("100"))
        );
    }

    @Test
    void currentAccountShouldNotEarnInterest() {
        CurrentAccount account = new CurrentAccount(
                "CUR1001", createCustomer(),
                new BigDecimal("5000"),
                new BigDecimal("1000")
        );

        assertEquals(
                new BigDecimal("0.00"),
                account.calculateInterest()
        );
    }

    @Test
    void fixedDepositShouldCalculateInterestForItsTenure() {
        FixedDepositAccount account = new FixedDepositAccount(
                "FDR1001", createCustomer(),
                new BigDecimal("12000"),
                new BigDecimal("6"),
                12
        );

        assertEquals(
                new BigDecimal("720.00"),
                account.calculateInterest()
        );
    }

    @Test
    void fixedDepositShouldRejectInvalidTenure() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FixedDepositAccount(
                        "FDR1002", createCustomer(),
                        new BigDecimal("12000"),
                        new BigDecimal("6"),
                        0
                )
        );
    }
}
