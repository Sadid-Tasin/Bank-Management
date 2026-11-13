package com.bank.bankmanagement.banking.service;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.transaction.enums.TransactionType;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankingServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private BankingService bankingService;

    @BeforeEach
    void setUp() {
        bankingService = new BankingService(
                accountRepository,
                transactionRepository
        );

    }

    private BankUser createCustomer() {
        return new BankUser(
                "Test Customer",
                "banking@test.com",
                "hashed-password",
                UserRole.CUSTOMER
        );
    }

    private SavingsAccount createAccount(
            String number,
            String openingBalance
    ) {
        return new SavingsAccount(
                number,
                createCustomer(),
                new BigDecimal(openingBalance),
                new BigDecimal("500"),
                new BigDecimal("5")
        );
    }

    @Test
    void depositShouldIncreaseBalanceAndCreateRecord() {
        Account account = createAccount("ACC-100", "1000");

        when(accountRepository.findByAccountNumberForUpdate("ACC-100"))
                .thenReturn(Optional.of(account));

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankTransaction result = bankingService.deposit(
                "ACC-100",
                new BigDecimal("200"),
                "Cash deposit"
        );

        assertEquals(new BigDecimal("1200.00"), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, result.getType());
        assertEquals(account, result.getDestinationAccount());
        verify(transactionRepository).save(any(BankTransaction.class));
    }

    @Test
    void withdrawalShouldDecreaseBalanceAndCreateRecord() {
        Account account = createAccount("ACC-101", "1000");

        when(accountRepository.findByAccountNumberForUpdate("ACC-101"))
                .thenReturn(Optional.of(account));

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankTransaction result = bankingService.withdraw(
                "ACC-101",
                new BigDecimal("200"),
                "Cash withdrawal"
        );

        assertEquals(new BigDecimal("800.00"), account.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, result.getType());
        assertEquals(account, result.getSourceAccount());
    }

    @Test
    void transferShouldMoveMoneyAndLockAccountsInOrder() {
        Account destination = createAccount("ACC-100", "500");
        Account source = createAccount("ACC-200", "1000");

        when(accountRepository.findByAccountNumberForUpdate("ACC-100"))
                .thenReturn(Optional.of(destination));

        when(accountRepository.findByAccountNumberForUpdate("ACC-200"))
                .thenReturn(Optional.of(source));

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankTransaction result = bankingService.transfer(
                "ACC-200",
                "ACC-100",
                new BigDecimal("200"),
                "Fund transfer"
        );

        assertEquals(new BigDecimal("800.00"), source.getBalance());
        assertEquals(new BigDecimal("700.00"), destination.getBalance());

        assertEquals(TransactionType.TRANSFER, result.getType());
        assertEquals(source, result.getSourceAccount());
        assertEquals(destination, result.getDestinationAccount());

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository)
                .findByAccountNumberForUpdate("ACC-100");
        order.verify(accountRepository)
                .findByAccountNumberForUpdate("ACC-200");
    }

    @Test
    void transferShouldRejectSameAccount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.transfer(
                        "ACC-100",
                        "ACC-100",
                        new BigDecimal("100"),
                        "Invalid transfer"
                )
        );

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void failedWithdrawalShouldNotChangeBalance() {
        Account source = createAccount("ACC-200", "1000");
        Account destination = createAccount("ACC-100", "500");

        when(accountRepository.findByAccountNumberForUpdate("ACC-100"))
                .thenReturn(Optional.of(destination));

        when(accountRepository.findByAccountNumberForUpdate("ACC-200"))
                .thenReturn(Optional.of(source));

        assertThrows(
                IllegalStateException.class,
                () -> bankingService.transfer(
                        "ACC-200",
                        "ACC-100",
                        new BigDecimal("600"),
                        "Insufficient balance"
                )
        );

        assertEquals(new BigDecimal("1000.00"), source.getBalance());
        assertEquals(new BigDecimal("500.00"), destination.getBalance());

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void operationShouldRejectUnknownAccount() {
        when(accountRepository.findByAccountNumberForUpdate("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.deposit(
                        "UNKNOWN",
                        new BigDecimal("100"),
                        "Test"
                )
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }
}
