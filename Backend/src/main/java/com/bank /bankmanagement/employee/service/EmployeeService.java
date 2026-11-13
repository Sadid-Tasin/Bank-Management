package com.bank.bankmanagement.employee.service;

import com.bank.bankmanagement.account.dto.AccountResponse;
import com.bank.bankmanagement.common.audit.AuditLogService;
import com.bank.bankmanagement.common.notification.NotificationService;
import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.banking.dto.BankingOperationRequest;
import com.bank.bankmanagement.banking.service.BankingService;
import com.bank.bankmanagement.employee.dto.EmployeeCustomerResponse;
import com.bank.bankmanagement.employee.dto.KycDecisionRequest;
import com.bank.bankmanagement.employee.dto.KycReviewResponse;
import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.transaction.entity.TransactionResponse;
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

import java.util.List;

@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<UserRole> EMPLOYEE_KYC_ROLES =
            List.of(UserRole.TELLER, UserRole.MANAGER, UserRole.ADMIN);

    private final BankUserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BankingService bankingService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public EmployeeService(
            BankUserRepository userRepository,
            AccountRepository accountRepository,
            BankingService bankingService,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.bankingService = bankingService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeCustomerResponse> listCustomers(int page, int size) {
        Pageable pageable = pageable(page, size);
        return userRepository.findByRole(UserRole.CUSTOMER, pageable)
                .map(EmployeeCustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<KycReviewResponse> listPendingKyc(int page, int size) {
        Pageable pageable = pageable(page, size);
        return userRepository.findByRoleAndKycStatus(
                        UserRole.CUSTOMER, KycStatus.PENDING, pageable)
                .map(KycReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public KycReviewResponse getCustomerKyc(Long customerId) {
        return KycReviewResponse.from(findCustomer(customerId));
    }

    @Transactional
    public KycReviewResponse reviewKyc(
            Long customerId, String reviewerEmail, KycDecisionRequest request
    ) {
        if (request == null || request.status() == null
                || request.status() == KycStatus.PENDING) {
            throw new IllegalArgumentException("KYC decision must be VERIFIED or REJECTED");
        }
        BankUser customer = findCustomer(customerId);
        customer.reviewKycStatus(request.status(), reviewerEmail, request.note());
        userRepository.save(customer);
        auditLogService.record(reviewerEmail, "KYC_" + request.status(), "CUSTOMER", customer.getId().toString(), request.note());
        notificationService.notifyUser(customer, "KYC reviewed", "Your KYC application status is " + request.status());
        return KycReviewResponse.from(customer);
    }

    @Transactional
    public TransactionResponse counterDeposit(
            String employeeEmail, BankingOperationRequest request
    ) {
        validateCounterRequest(request);
        validateCustomerAccount(request.accountNumber());
        String description = counterDescription("Counter deposit", employeeEmail,
                request.description());
        BankTransaction transaction = bankingService.deposit(
                request.accountNumber(), request.amount(), description, employeeEmail);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse counterWithdraw(
            String employeeEmail, BankingOperationRequest request
    ) {
        validateCounterRequest(request);
        validateCustomerAccount(request.accountNumber());
        String description = counterDescription("Counter withdrawal", employeeEmail,
                request.description());
        BankTransaction transaction = bankingService.withdraw(
                request.accountNumber(), request.amount(), description, employeeEmail);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public AccountResponse freezeAccount(String accountNumber) {
        return freezeAccount(accountNumber, "authorized-manager");
    }

    @Transactional
    public AccountResponse freezeAccount(String accountNumber, String managerEmail) {
        Account account = findAccountForUpdate(accountNumber);
        account.freeze();
        accountRepository.save(account);
        auditLogService.record(managerEmail, "ACCOUNT_FROZEN", "ACCOUNT", accountNumber, "Account frozen by authorized manager");
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse unfreezeAccount(String accountNumber) {
        return unfreezeAccount(accountNumber, "authorized-manager");
    }

    @Transactional
    public AccountResponse unfreezeAccount(String accountNumber, String managerEmail) {
        Account account = findAccountForUpdate(accountNumber);
        account.unfreeze();
        accountRepository.save(account);
        auditLogService.record(managerEmail, "ACCOUNT_UNFROZEN", "ACCOUNT", accountNumber, "Account unfrozen by authorized manager");
        return AccountResponse.from(account);
    }

    private BankUser findCustomer(Long customerId) {
        BankUser user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found"));
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Customer not found");
        }
        return user;
    }

    private void validateCustomerAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));
        if (account.getOwner().getRole() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Counter transactions are limited to customer accounts");
        }
        if (account.getOwner().getKycStatus() != KycStatus.VERIFIED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Customer KYC must be verified");
        }
    }

    private Account findAccountForUpdate(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        return accountRepository.findByAccountNumberForUpdate(accountNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));
    }

    private void validateCounterRequest(BankingOperationRequest request) {
        if (request == null || request.accountNumber() == null
                || request.accountNumber().isBlank() || request.amount() == null) {
            throw new IllegalArgumentException("Account number and amount are required");
        }
    }

    private String counterDescription(String action, String employeeEmail, String note) {
        String identity = employeeEmail == null || employeeEmail.isBlank()
                ? "authorized employee" : employeeEmail.trim();
        String value = action + " by " + identity;
        if (note != null && !note.isBlank()) {
            value += " - " + note.trim();
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    private Pageable pageable(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        return PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
