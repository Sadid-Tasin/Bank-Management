package com.bank.bankmanagement.account.factory;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.account.entity.CurrentAccount;
import com.bank.bankmanagement.account.entity.FixedDepositAccount;
import com.bank.bankmanagement.account.enums.AccountType;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountFactoryTest {

    private final AccountFactory factory = new AccountFactory();

    private BankUser createCustomer() {
        return new BankUser(
                "Test Customer",
                "factory@test.com",
                "hashed-password",
                UserRole.CUSTOMER
        );
    }

    @Test
    void shouldCreateSavingsAccount() {
        Account account = factory.createAccount(
                AccountType.SAVINGS,
                "SAV2001",
                createCustomer(),
                new BigDecimal("5000"),
                new BigDecimal("500"),
                new BigDecimal("5"),
                null
        );

        assertInstanceOf(SavingsAccount.class, account);
        assertEquals(
                new BigDecimal("5000.00"),
                account.getBalance()
        );
    }

    @Test
    void shouldCreateCurrentAccount() {
        Account account = factory.createAccount(
                AccountType.CURRENT,
                "CUR2001",
                createCustomer(),
                new BigDecimal("10000"),
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                null
        );

        assertInstanceOf(CurrentAccount.class, account);
    }

    @Test
    void shouldCreateFixedDepositAccount() {
        Account account = factory.createAccount(
                AccountType.FIXED_DEPOSIT,
                "FDR2001",
                createCustomer(),
                new BigDecimal("20000"),
                BigDecimal.ZERO,
                new BigDecimal("6"),
                12
        );

        assertInstanceOf(FixedDepositAccount.class, account);

        assertEquals(
                new BigDecimal("1200.00"),
                account.calculateInterest()
        );
    }

    @Test
    void shouldRejectInvalidFixedDepositTenure() {
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.createAccount(
                        AccountType.FIXED_DEPOSIT,
                        "FDR2002",
                        createCustomer(),
                        new BigDecimal("20000"),
                        BigDecimal.ZERO,
                        new BigDecimal("6"),
                        0
                )
        );
    }

    @Test
    void shouldRejectMissingAccountType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.createAccount(
                        null,
                        "TEST2001",
                        createCustomer(),
                        new BigDecimal("5000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null
                )
        );
    }
}
