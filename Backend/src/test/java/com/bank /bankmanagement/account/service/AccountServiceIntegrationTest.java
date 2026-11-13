package com.bank.bankmanagement.account.service;

import com.bank.bankmanagement.account.dto.AccountResponse;
import com.bank.bankmanagement.account.dto.CreateAccountRequest;
import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.account.enums.AccountType;
import com.bank.bankmanagement.account.factory.AccountFactory;
import com.bank.bankmanagement.account.repository.AccountRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankUserRepository userRepository;

    @Autowired
    private AccountFactory accountFactory;

    private BankUser customer;
    private final List<BankUser> extraUsers = new ArrayList<>();

    @BeforeEach
    void createCustomer() {
        String email = "account-" +
                UUID.randomUUID().toString().replace("-", "")
                + "@example.test";

        customer = userRepository.saveAndFlush(
                new BankUser(
                        "Account Test Customer",
                        email,
                        "test-password-hash",
                        UserRole.CUSTOMER
                )
        );
    }

    @AfterEach
    void cleanup() {
        accountRepository.deleteAllByOwner_Id(customer.getId());

        for (BankUser extra : extraUsers) {
            accountRepository.deleteAllByOwner_Id(extra.getId());
            userRepository.deleteById(extra.getId());
        }

        extraUsers.clear();
        userRepository.deleteById(customer.getId());
    }

    private void verifyCustomer() {
        customer.updateKycStatus(KycStatus.VERIFIED);
        customer = userRepository.saveAndFlush(customer);
    }

    private AccountResponse createSavings() {
        return accountService.createAccount(
                customer.getEmail(),
                new CreateAccountRequest(
                        AccountType.SAVINGS,
                        new BigDecimal("5000.00"),
                        null
                )
        );
    }

    @Test
    void verifiedCustomerCanCreateSavingsAccount() {
        verifyCustomer();

        AccountResponse response = createSavings();

        assertNotNull(response.id());
        assertNotNull(response.accountNumber());
        assertEquals(AccountType.SAVINGS, response.accountType());
        assertEquals(
                new BigDecimal("5000.00"),
                response.balance()
        );
        assertEquals("ACTIVE", response.status().name());
    }

    @Test
    void pendingKycCustomerCannotOpenAccount() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                this::createSavings
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(accountService.getMyAccounts(
                customer.getEmail()).isEmpty());
    }

    @Test
    void verifiedCustomerCanCreateFixedDeposit() {
        verifyCustomer();

        AccountResponse response = accountService.createAccount(
                customer.getEmail(),
                new CreateAccountRequest(
                        AccountType.FIXED_DEPOSIT,
                        new BigDecimal("20000.00"),
                        12
                )
        );

        assertEquals(AccountType.FIXED_DEPOSIT, response.accountType());
        assertEquals(12, response.tenureMonths());
        assertEquals(
                new BigDecimal("6.00"),
                response.annualInterestRate()
        );
    }

    @Test
    void fixedDepositRequiresValidTenure() {
        verifyCustomer();

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(
                        customer.getEmail(),
                        new CreateAccountRequest(
                                AccountType.FIXED_DEPOSIT,
                                new BigDecimal("20000.00"),
                                0
                        )
                )
        );
    }

    @Test
    void customerCannotOpenSavingsBelowMinimumBalance() {
        verifyCustomer();

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(
                        customer.getEmail(),
                        new CreateAccountRequest(
                                AccountType.SAVINGS,
                                new BigDecimal("400.00"),
                                null
                        )
                )
        );
    }

    @Test
    void customerCannotReadAnotherCustomersAccount() {
        verifyCustomer();

        BankUser anotherCustomer = userRepository.saveAndFlush(
                new BankUser(
                        "Another Customer",
                        "other-" + UUID.randomUUID() + "@example.test",
                        "test-password-hash",
                        UserRole.CUSTOMER
                )
        );

        extraUsers.add(anotherCustomer);
        anotherCustomer.updateKycStatus(KycStatus.VERIFIED);
        userRepository.saveAndFlush(anotherCustomer);

        SavingsAccount foreignAccount = new SavingsAccount(
                "FOREIGN-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 12),
                anotherCustomer,
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("5.00")
        );

        accountRepository.saveAndFlush(foreignAccount);

        assertThrows(
                ResponseStatusException.class,
                () -> accountService.getBalance(
                        customer.getEmail(),
                        foreignAccount.getAccountNumber()
                )
        );

        assertTrue(accountService.getMyAccounts(
                customer.getEmail()).isEmpty());
    }

    @Test
    void accountListShouldContainOnlyOwnAccounts() {
        verifyCustomer();

        createSavings();
        createSavings();

        assertEquals(
                2,
                accountService.getMyAccounts(
                        customer.getEmail()).size()
        );
    }
}
