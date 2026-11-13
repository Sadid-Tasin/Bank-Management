package com.bank.bankmanagement.banking.service;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BankingServiceIntegrationTest {

    @Autowired
    private BankingService bankingService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankUserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate transactionTemplate;
    private TestFixture fixture;

    private record TestFixture(
            String email,
            String sourceAccount,
            String destinationAccount
    ) {}

    @BeforeEach
    void createTestAccounts() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16);

        String email = "integration-" + suffix + "@example.test";
        String source = "IT-S-" + suffix;
        String destination = "IT-D-" + suffix;

        fixture = transactionTemplate.execute(status -> {
            BankUser customer = userRepository.saveAndFlush(
                    new BankUser(
                            "Integration Test Customer",
                            email,
                            "test-password-hash",
                            UserRole.CUSTOMER
                    )
            );

            accountRepository.saveAndFlush(
                    new SavingsAccount(
                            source,
                            customer,
                            new BigDecimal("1000.00"),
                            new BigDecimal("100.00"),
                            new BigDecimal("5.00")
                    )
            );

            accountRepository.saveAndFlush(
                    new SavingsAccount(
                            destination,
                            customer,
                            new BigDecimal("500.00"),
                            BigDecimal.ZERO,
                            new BigDecimal("5.00")
                    )
            );

            return new TestFixture(email, source, destination);
        });

        if (fixture == null) {
            throw new IllegalStateException("Test account setup failed");
        }
    }

    @AfterEach
    void removeOnlyTestData() {
        if (fixture == null || transactionTemplate == null) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("""
                    DELETE FROM bank_transactions
                    WHERE source_account_id IN (
                        SELECT id FROM bank_accounts
                        WHERE account_number IN (:source, :destination)
                    )
                    OR destination_account_id IN (
                        SELECT id FROM bank_accounts
                        WHERE account_number IN (:source, :destination)
                    )
                    """)
                    .setParameter("source", fixture.sourceAccount())
                    .setParameter("destination", fixture.destinationAccount())
                    .executeUpdate();

            entityManager.createNativeQuery("""
                    DELETE FROM bank_accounts
                    WHERE account_number IN (:source, :destination)
                    """)
                    .setParameter("source", fixture.sourceAccount())
                    .setParameter("destination", fixture.destinationAccount())
                    .executeUpdate();

            entityManager.createNativeQuery("""
                    DELETE FROM bank_users WHERE email = :email
                    """)
                    .setParameter("email", fixture.email())
                    .executeUpdate();
        });
    }

    private BigDecimal readBalance(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow()
                .getBalance();
    }

    private long countTransactions() {
        Long count = transactionTemplate.execute(status -> {
            Number result = (Number) entityManager.createNativeQuery("""
                    SELECT COUNT(DISTINCT t.id)
                    FROM bank_transactions t
                    JOIN bank_accounts a
                      ON a.id = t.source_account_id
                      OR a.id = t.destination_account_id
                    WHERE a.account_number IN (:source, :destination)
                    """)
                    .setParameter("source", fixture.sourceAccount())
                    .setParameter("destination", fixture.destinationAccount())
                    .getSingleResult();

            return result.longValue();
        });

        return count == null ? 0 : count;
    }

    @Test
    void depositShouldPersistBalanceAndTransaction() {
        BankTransaction transaction = bankingService.deposit(
                fixture.sourceAccount(),
                new BigDecimal("200.00"),
                "Integration test deposit"
        );

        assertNotNull(transaction.getReference());
        assertEquals(
                new BigDecimal("1200.00"),
                readBalance(fixture.sourceAccount())
        );
        assertEquals(1, countTransactions());
    }

    @Test
    void withdrawalShouldPersistBalanceAndTransaction() {
        bankingService.withdraw(
                fixture.sourceAccount(),
                new BigDecimal("200.00"),
                "Integration test withdrawal"
        );

        assertEquals(
                new BigDecimal("800.00"),
                readBalance(fixture.sourceAccount())
        );
        assertEquals(1, countTransactions());
    }

    @Test
    void transferShouldPersistBothBalancesAndTransaction() {
        bankingService.transfer(
                fixture.sourceAccount(),
                fixture.destinationAccount(),
                new BigDecimal("200.00"),
                "Integration test transfer"
        );

        assertEquals(
                new BigDecimal("800.00"),
                readBalance(fixture.sourceAccount())
        );

        assertEquals(
                new BigDecimal("700.00"),
                readBalance(fixture.destinationAccount())
        );

        assertEquals(1, countTransactions());
    }

    @Test
    void failedTransferShouldRollbackBothAccounts() {
        Account destination = accountRepository
                .findByAccountNumber(fixture.destinationAccount())
                .orElseThrow();

        destination.freeze();
        accountRepository.saveAndFlush(destination);

        assertThrows(
                IllegalStateException.class,
                () -> bankingService.transfer(
                        fixture.sourceAccount(),
                        fixture.destinationAccount(),
                        new BigDecimal("200.00"),
                        "Test rollback"
                )
        );

        assertEquals(
                new BigDecimal("1000.00"),
                readBalance(fixture.sourceAccount())
        );

        assertEquals(
                new BigDecimal("500.00"),
                readBalance(fixture.destinationAccount())
        );

        assertEquals(0, countTransactions());
    }

    @Test
    void insufficientBalanceShouldNotCreateTransaction() {
        assertThrows(
                IllegalStateException.class,
                () -> bankingService.withdraw(
                        fixture.sourceAccount(),
                        new BigDecimal("950.00"),
                        "Insufficient balance test"
                )
        );

        assertEquals(
                new BigDecimal("1000.00"),
                readBalance(fixture.sourceAccount())
        );

        assertEquals(0, countTransactions());
    }
}
