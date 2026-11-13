package com.bank.bankmanagement.account.service;

import com.bank.bankmanagement.account.dto.AccountResponse;
import com.bank.bankmanagement.common.audit.AuditLogService;
import com.bank.bankmanagement.account.dto.BalanceResponse;
import com.bank.bankmanagement.account.dto.CreateAccountRequest;
import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.enums.AccountType;
import com.bank.bankmanagement.account.factory.AccountFactory;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.transaction.entity.TransactionResponse;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private static final BigDecimal SAVINGS_MINIMUM = new BigDecimal("500.00");
    private static final BigDecimal CURRENT_MINIMUM = new BigDecimal("1000.00");
    private static final BigDecimal FIXED_DEPOSIT_MINIMUM = new BigDecimal("10000.00");
    private static final BigDecimal SAVINGS_RATE = new BigDecimal("5.00");
    private static final BigDecimal CURRENT_RATE = BigDecimal.ZERO;
    private static final BigDecimal FIXED_DEPOSIT_RATE = new BigDecimal("6.00");
    private static final int MAX_PAGE_SIZE = 50;

    private final AccountRepository accountRepository;
    private final BankUserRepository userRepository;
    private final AccountFactory accountFactory;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    public AccountService(
            AccountRepository accountRepository,
            BankUserRepository userRepository,
            AccountFactory accountFactory,
            TransactionRepository transactionRepository,
            AuditLogService auditLogService
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountFactory = accountFactory;
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AccountResponse createAccount(String email, CreateAccountRequest request) {
        BankUser customer = findUser(email);

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only customers can open personal accounts");
        }
        if (customer.getKycStatus() != KycStatus.VERIFIED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "KYC verification is required before opening an account");
        }
        if (request == null || request.accountType() == null
                || request.openingBalance() == null) {
            throw new IllegalArgumentException("Account type and opening balance are required");
        }

        BigDecimal minimumBalance;
        BigDecimal rate;
        switch (request.accountType()) {
            case SAVINGS -> {
                minimumBalance = SAVINGS_MINIMUM;
                rate = SAVINGS_RATE;
            }
            case CURRENT -> {
                minimumBalance = CURRENT_MINIMUM;
                rate = CURRENT_RATE;
            }
            case FIXED_DEPOSIT -> {
                minimumBalance = FIXED_DEPOSIT_MINIMUM;
                rate = FIXED_DEPOSIT_RATE;
                if (request.tenureMonths() == null
                        || request.tenureMonths() < 1
                        || request.tenureMonths() > 60) {
                    throw new IllegalArgumentException(
                            "Fixed deposit tenure must be between 1 and 60 months");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported account type");
        }

        if (request.openingBalance().compareTo(minimumBalance) < 0) {
            throw new IllegalArgumentException(
                    "Opening balance must be at least " + minimumBalance.toPlainString());
        }
        if (request.openingBalance().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Opening balance cannot have more than two decimal places");
        }

        String accountNumber = generateUniqueAccountNumber();
        Account account = accountFactory.createAccount(
                request.accountType(),
                accountNumber,
                customer,
                request.openingBalance(),
                minimumBalance,
                rate,
                request.tenureMonths()
        );

        Account saved = accountRepository.save(account);
        auditLogService.record(email, "ACCOUNT_OPENED", "ACCOUNT", saved.getAccountNumber(),
                "Type=" + saved.getAccountType());
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String email) {
        BankUser customer = findUser(email);
        return accountRepository.findAllByOwner_Id(customer.getId())
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String email, String accountNumber) {
        Account account = findOwnedAccount(email, accountNumber);
        return new BalanceResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getMinimumBalance(),
                account.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(
            String email, String accountNumber, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 50");
        }

        Account account = findOwnedAccount(email, accountNumber);
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findHistoryForAccountAndOwner(
                        account.getAccountNumber(), account.getOwner().getId(), pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getMiniStatement(
            String email, String accountNumber) {
        return getTransactionHistory(email, accountNumber, 0, 5).getContent();
    }

    @Transactional(readOnly = true)
    public Account findOwnedAccount(String email, String accountNumber) {
        BankUser customer = findUser(email);
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }

        return accountRepository.findByAccountNumberAndOwner_Id(
                        accountNumber.trim(), customer.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));
    }

    private BankUser findUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "10" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 12).toUpperCase();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique account number");
    }
}
