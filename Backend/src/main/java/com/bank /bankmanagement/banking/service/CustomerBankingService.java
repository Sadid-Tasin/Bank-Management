package com.bank.bankmanagement.banking.service;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class CustomerBankingService {

    private final AccountRepository accountRepository;
    private final BankUserRepository userRepository;
    private final BankingService bankingService;

    public CustomerBankingService(
            AccountRepository accountRepository,
            BankUserRepository userRepository,
            BankingService bankingService
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.bankingService = bankingService;
    }

    @Transactional
    public BankTransaction deposit(
            String customerEmail, String accountNumber,
            BigDecimal amount, String description) {
        requireOwnedAccount(customerEmail, accountNumber);
        return bankingService.deposit(accountNumber, amount, description, customerEmail);
    }

    @Transactional
    public BankTransaction withdraw(
            String customerEmail, String accountNumber,
            BigDecimal amount, String description) {
        requireOwnedAccount(customerEmail, accountNumber);
        return bankingService.withdraw(accountNumber, amount, description, customerEmail);
    }

    @Transactional
    public BankTransaction transfer(
            String customerEmail, String sourceAccountNumber,
            String destinationAccountNumber, BigDecimal amount,
            String description) {
        requireOwnedAccount(customerEmail, sourceAccountNumber);
        return bankingService.transfer(
                sourceAccountNumber, destinationAccountNumber, amount, description, customerEmail);
    }

    private Account requireOwnedAccount(String email, String accountNumber) {
        BankUser customer = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found"));
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        return accountRepository.findByAccountNumberAndOwner_Id(
                        accountNumber.trim(), customer.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));
    }
}
