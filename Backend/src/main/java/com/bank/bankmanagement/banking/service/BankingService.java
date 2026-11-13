package com.bank.bankmanagement.banking.service;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.common.audit.AuditLogService;
import com.bank.bankmanagement.common.fraud.FraudDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.transaction.enums.TransactionType;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class BankingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final FraudDetectionService fraudDetectionService;

    @Autowired
    public BankingService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          AuditLogService auditLogService,
                          FraudDetectionService fraudDetectionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
        this.fraudDetectionService = fraudDetectionService;
    }

    // Retained for focused unit tests which exercise only the core ledger logic.
    public BankingService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogService = null;
        this.fraudDetectionService = null;
    }

    @Transactional
    public BankTransaction deposit(
            String accountNumber,
            BigDecimal amount,
            String description
    ) {
        return deposit(accountNumber, amount, description, null);
    }

    @Transactional
    public BankTransaction deposit(String accountNumber, BigDecimal amount,
                                   String description, String actorEmail) {
        Account account = findAccountForUpdate(accountNumber);

        account.deposit(amount);

        BankTransaction transaction = new BankTransaction(
                TransactionType.DEPOSIT,
                null,
                account,
                amount,
                description
        );

        BankTransaction saved = transactionRepository.save(transaction);
        recordControls(saved, actorEmail, "DEPOSIT");
        return saved;
    }

    @Transactional
    public BankTransaction withdraw(
            String accountNumber,
            BigDecimal amount,
            String description
    ) {
        return withdraw(accountNumber, amount, description, null);
    }

    @Transactional
    public BankTransaction withdraw(String accountNumber, BigDecimal amount,
                                    String description, String actorEmail) {
        Account account = findAccountForUpdate(accountNumber);

        account.withdraw(amount);

        BankTransaction transaction = new BankTransaction(
                TransactionType.WITHDRAWAL,
                account,
                null,
                amount,
                description
        );

        BankTransaction saved = transactionRepository.save(transaction);
        recordControls(saved, actorEmail, "WITHDRAWAL");
        return saved;
    }

    @Transactional
    public BankTransaction transfer(
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String description
    ) {
        return transfer(sourceAccountNumber, destinationAccountNumber, amount, description, null);
    }

    @Transactional
    public BankTransaction transfer(String sourceAccountNumber,
                                    String destinationAccountNumber,
                                    BigDecimal amount, String description,
                                    String actorEmail) {
        String sourceNumber = validateAccountNumber(sourceAccountNumber);
        String destinationNumber = validateAccountNumber(destinationAccountNumber);

        if (sourceNumber.equals(destinationNumber)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different");
        }

        // Always acquire account locks in the same order.
        String firstNumber = sourceNumber.compareTo(destinationNumber) < 0
                ? sourceNumber : destinationNumber;

        String secondNumber = sourceNumber.compareTo(destinationNumber) < 0
                ? destinationNumber : sourceNumber;

        Account firstAccount = findAccountForUpdate(firstNumber);
        Account secondAccount = findAccountForUpdate(secondNumber);

        Account sourceAccount = sourceNumber.equals(firstNumber)
                ? firstAccount : secondAccount;

        Account destinationAccount = destinationNumber.equals(firstNumber)
                ? firstAccount : secondAccount;

        // Both changes happen inside the same database transaction.
        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);

        BankTransaction transaction = new BankTransaction(
                TransactionType.TRANSFER,
                sourceAccount,
                destinationAccount,
                amount,
                description
        );

        BankTransaction saved = transactionRepository.save(transaction);
        recordControls(saved, actorEmail, "TRANSFER");
        return saved;
    }

    private void recordControls(BankTransaction transaction, String actorEmail, String action) {
        if (auditLogService != null) {
            String accountNo = transaction.getSourceAccount() != null
                    ? transaction.getSourceAccount().getAccountNumber()
                    : transaction.getDestinationAccount().getAccountNumber();
            auditLogService.record(actorEmail, "BANKING_" + action, "TRANSACTION",
                    transaction.getReference(), "Account=" + accountNo + ", amount=" + transaction.getAmount());
        }
        if (fraudDetectionService != null) fraudDetectionService.inspect(transaction);
    }

    private Account findAccountForUpdate(String accountNumber) {
        String validNumber = validateAccountNumber(accountNumber);

        return accountRepository.findByAccountNumberForUpdate(validNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));
    }

    private String validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Account number is required");
        }

        return accountNumber.trim();
    }
}
