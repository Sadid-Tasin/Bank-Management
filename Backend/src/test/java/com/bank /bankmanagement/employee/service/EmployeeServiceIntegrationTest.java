package com.bank.bankmanagement.employee.service;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.enums.AccountType;
import com.bank.bankmanagement.account.factory.AccountFactory;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.common.notification.NotificationRepository;
import com.bank.bankmanagement.banking.dto.BankingOperationRequest;
import com.bank.bankmanagement.employee.dto.KycDecisionRequest;
import com.bank.bankmanagement.employee.dto.KycReviewResponse;
import com.bank.bankmanagement.transaction.entity.TransactionResponse;
import com.bank.bankmanagement.transaction.enums.TransactionType;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeServiceIntegrationTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private BankUserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private AccountFactory accountFactory;
    @Autowired private NotificationRepository notificationRepository;

    private String suffix;
    private String customerEmail;
    private String tellerEmail;
    private String accountNumber;
    private Long customerId;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        customerEmail = "employee-customer-" + suffix + "@example.test";
        tellerEmail = "teller-" + suffix + "@example.test";
        accountNumber = "31" + suffix.substring(0, 12).toUpperCase();

        BankUser customer = new BankUser(
                "Employee Test Customer", customerEmail, "test-hash", UserRole.CUSTOMER);
        customer.updateContactDetails("01710000000", "E-NID-" + suffix, "Dhaka");
        customer = userRepository.save(customer);
        customerId = customer.getId();

        BankUser teller = new BankUser(
                "Employee Test Teller", tellerEmail, "test-hash", UserRole.TELLER);
        userRepository.save(teller);
    }

    @AfterEach
    void cleanUp() {
        if (accountNumber != null) {
            transactionRepository.deleteAllForAccountNumbers(List.of(accountNumber));
            accountRepository.findByAccountNumber(accountNumber).ifPresent(accountRepository::delete);
        }
        if (customerEmail != null) {
            userRepository.findByEmailIgnoreCase(customerEmail).ifPresent(user -> {
                notificationRepository.deleteAllByUserId(user.getId());
                userRepository.delete(user);
            });
        }
        if (tellerEmail != null) {
            userRepository.findByEmailIgnoreCase(tellerEmail).ifPresent(userRepository::delete);
        }
    }

    private Account createCustomerAccount(boolean verified) {
        BankUser customer = userRepository.findById(customerId).orElseThrow();
        if (verified) customer.updateKycStatus(KycStatus.VERIFIED);
        userRepository.save(customer);
        Account account = accountFactory.createAccount(
                AccountType.SAVINGS, accountNumber, customer,
                new BigDecimal("2500.00"), new BigDecimal("500.00"),
                new BigDecimal("5.00"), null);
        return accountRepository.save(account);
    }

    @Test
    void pendingKycShouldBeListedAndCanBeApprovedWithReviewerDetails() {
        assertTrue(employeeService.listPendingKyc(0, 100).getContent().stream()
                .anyMatch(item -> item.customerId().equals(customerId)));

        KycReviewResponse response = employeeService.reviewKyc(
                customerId, tellerEmail,
                new KycDecisionRequest(KycStatus.VERIFIED, "Documents checked"));

        assertEquals(KycStatus.VERIFIED, response.kycStatus());
        assertEquals(tellerEmail, response.reviewedByEmail());
        assertEquals("Documents checked", response.reviewNote());
        assertNotNull(response.reviewedAt());
    }

    @Test
    void pendingKycCanBeRejectedWithReason() {
        KycReviewResponse response = employeeService.reviewKyc(
                customerId, tellerEmail,
                new KycDecisionRequest(KycStatus.REJECTED, "Document image is unclear"));

        assertEquals(KycStatus.REJECTED, response.kycStatus());
        assertEquals("Document image is unclear", response.reviewNote());
    }

    @Test
    void alreadyReviewedKycCannotBeReviewedAgain() {
        employeeService.reviewKyc(customerId, tellerEmail,
                new KycDecisionRequest(KycStatus.VERIFIED, "Verified"));

        assertThrows(IllegalStateException.class, () -> employeeService.reviewKyc(
                customerId, tellerEmail,
                new KycDecisionRequest(KycStatus.REJECTED, "Second decision")));
    }

    @Test
    void tellerCounterDepositShouldCreateTransactionAndIncreaseBalance() {
        Account account = createCustomerAccount(true);

        TransactionResponse response = employeeService.counterDeposit(
                tellerEmail,
                new BankingOperationRequest(accountNumber, new BigDecimal("500.00"), "Cash received"));

        Account saved = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertEquals(new BigDecimal("3000.00"), saved.getBalance());
        assertEquals(TransactionType.DEPOSIT, response.type());
        assertTrue(response.description().contains(tellerEmail));
    }

    @Test
    void tellerCounterWithdrawalShouldDecreaseBalance() {
        createCustomerAccount(true);

        TransactionResponse response = employeeService.counterWithdraw(
                tellerEmail,
                new BankingOperationRequest(accountNumber, new BigDecimal("400.00"), "Cash paid"));

        Account saved = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertEquals(new BigDecimal("2100.00"), saved.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, response.type());
    }

    @Test
    void counterTransactionMustRejectUnverifiedCustomer() {
        createCustomerAccount(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> employeeService.counterDeposit(tellerEmail,
                        new BankingOperationRequest(accountNumber,
                                new BigDecimal("100.00"), null)));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void managerCanFreezeAndUnfreezeAccount() {
        createCustomerAccount(true);

        assertEquals(com.bank.bankmanagement.account.enums.AccountStatus.FROZEN,
                employeeService.freezeAccount(accountNumber).status());
        assertEquals(com.bank.bankmanagement.account.enums.AccountStatus.ACTIVE,
                employeeService.unfreezeAccount(accountNumber).status());
    }

    @Test
    void frozenAccountCannotAcceptCounterDeposit() {
        createCustomerAccount(true);
        employeeService.freezeAccount(accountNumber);

        assertThrows(IllegalStateException.class, () -> employeeService.counterDeposit(
                tellerEmail, new BankingOperationRequest(accountNumber,
                        new BigDecimal("100.00"), null)));
    }
}
